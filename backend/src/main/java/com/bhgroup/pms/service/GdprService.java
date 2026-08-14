package com.bhgroup.pms.service;

import com.bhgroup.pms.common.PiiMasking;
import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.config.AppProperties;
import com.bhgroup.pms.domain.AuditAction;
import com.bhgroup.pms.domain.GdprRecordType;
import com.bhgroup.pms.domain.GdprRequest;
import com.bhgroup.pms.domain.GdprRequestType;
import com.bhgroup.pms.domain.GdprVerificationMethod;
import com.bhgroup.pms.domain.LateCheckoutRequest;
import com.bhgroup.pms.domain.Message;
import com.bhgroup.pms.domain.PropertyLead;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.dto.gdpr.GdprEraseResultResponse;
import com.bhgroup.pms.dto.gdpr.GdprExportResponse;
import com.bhgroup.pms.dto.gdpr.GdprLateCheckoutExport;
import com.bhgroup.pms.dto.gdpr.GdprLeadExport;
import com.bhgroup.pms.dto.gdpr.GdprMessageExport;
import com.bhgroup.pms.dto.gdpr.GdprReservationExport;
import com.bhgroup.pms.dto.gdpr.GdprSearchMatchResponse;
import com.bhgroup.pms.repository.GdprRequestRepository;
import com.bhgroup.pms.repository.LateCheckoutRequestRepository;
import com.bhgroup.pms.repository.MessageRepository;
import com.bhgroup.pms.repository.NotificationRepository;
import com.bhgroup.pms.repository.PropertyLeadRepository;
import com.bhgroup.pms.repository.ReservationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Technical support for data-subject-access and erasure requests
 * (GDPR arts. 15/17): guests aren't user accounts here, so requests are
 * looked up by email across reservations and property leads rather than
 * by a user id.
 *
 * Erasure anonymizes rather than deletes reservations - the row itself
 * (dates, amounts, property, status) is kept for fiscal record-keeping,
 * only the personally-identifying fields are cleared. This matches what
 * the public privacy policy already promises, but the exact legally
 * required retention period is a legal question, not a technical one -
 * this only implements the mechanism, on demand.
 *
 * Erasure also has to reach every copy of the guest's PII, not just the
 * Reservation row it originates from: the unauthenticated
 * {@code managementToken} link (still usable to view/modify/cancel/
 * message the booking otherwise), every message in the thread (staff
 * replies can restate the guest's own details back at them, not just
 * their own messages), the free-standing Notification rows a guest
 * message (or a late checkout request) fans out to (title/body copied in
 * at creation time, no FK back to the reservation), and the free-text
 * {@code guestNote} on any {@link com.bhgroup.pms.domain.LateCheckoutRequest}
 * tied to the reservation - a guest can write anything in it, including
 * their own name or phone number, via the unauthenticated
 * management-token link.
 *
 * Neither the general audit log nor the {@link GdprRequest} compliance
 * register ever get the full email - only {@link PiiMasking#maskEmail}'d
 * form plus a keyed {@link PiiMasking#fingerprintEmail} for exact-match
 * verification. The whole point of this feature is to stop retaining a
 * data subject's PII on request; logging it in full here would just move
 * the problem, not solve it. The audit log write is deliberately deferred
 * to run only after this method's own transaction commits (see
 * {@link #afterSuccessfulCommit}) - {@link AuditService#record} uses
 * REQUIRES_NEW and commits independently, so calling it eagerly would let
 * "GDPR_DATA_ERASED" reach the audit log even on a request whose
 * anonymization was later rolled back.
 */
@Service
@RequiredArgsConstructor
public class GdprService {

    private static final String REDACTED_MESSAGE = "[mesaj șters - solicitare GDPR]";
    private static final String REDACTED_NOTIFICATION_TITLE = "Mesaj șters (solicitare GDPR)";
    private static final String REDACTED_NOTIFICATION_BODY = "[conținut șters - solicitare GDPR]";
    /** Romanian CNP (and most national ID numbers) are long runs of digits - a cheap, effective tripwire. */
    private static final Pattern LOOKS_LIKE_ID_NUMBER = Pattern.compile("\\d{8,}");
    private static final int MAX_VERIFICATION_NOTE_LENGTH = 300;

    private final ReservationRepository reservationRepository;
    private final PropertyLeadRepository propertyLeadRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final LateCheckoutRequestRepository lateCheckoutRequestRepository;
    private final GdprRequestRepository gdprRequestRepository;
    private final AuditService auditService;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public List<GdprSearchMatchResponse> search(String email) {
        List<GdprSearchMatchResponse> results = new java.util.ArrayList<>();

        for (Reservation r : reservationRepository.findByGuestEmailIgnoreCase(email)) {
            results.add(new GdprSearchMatchResponse(
                    GdprRecordType.RESERVATION, r.getId(), r.getGuestFullName(), r.getGuestEmail(),
                    r.getGuestPhone(), r.getProperty().getName() + " (" + r.getCheckInDate() + " - " + r.getCheckOutDate() + ")",
                    r.getCreatedAt()));
        }
        for (PropertyLead lead : propertyLeadRepository.findByEmailIgnoreCase(email)) {
            results.add(new GdprSearchMatchResponse(
                    GdprRecordType.LEAD, lead.getId(), lead.getFullName(), lead.getEmail(),
                    lead.getPhone(), "Lead: " + (lead.getCity() != null ? lead.getCity() : "-"),
                    lead.getCreatedAt()));
        }
        return results;
    }

    @Transactional
    public GdprExportResponse export(String email, GdprVerificationMethod verificationMethod,
                                      String verificationNote, User actor) {
        requireValidVerification(verificationMethod, verificationNote);

        List<Reservation> reservations = reservationRepository.findByGuestEmailIgnoreCase(email);
        List<PropertyLead> leads = propertyLeadRepository.findByEmailIgnoreCase(email);
        if (reservations.isEmpty() && leads.isEmpty()) {
            throw new BadRequestException("Nu s-au găsit înregistrări pentru acest email");
        }

        List<UUID> reservationIdsForExport = reservations.stream().map(Reservation::getId).toList();

        Map<UUID, List<Message>> messagesByReservation = reservationIdsForExport.isEmpty()
                ? Map.of()
                : messageRepository.findByReservationIdInOrderByCreatedAtAsc(reservationIdsForExport)
                    .stream().collect(Collectors.groupingBy(m -> m.getReservation().getId()));

        Map<UUID, List<LateCheckoutRequest>> lateCheckoutByReservation = reservationIdsForExport.isEmpty()
                ? Map.of()
                : lateCheckoutRequestRepository.findByReservationIdIn(reservationIdsForExport)
                    .stream().collect(Collectors.groupingBy(r -> r.getReservation().getId()));

        List<GdprReservationExport> reservationExports = reservations.stream()
                .map(r -> new GdprReservationExport(
                        r.getId(), r.getProperty().getName(), r.getGuestFirstName(), r.getGuestLastName(),
                        r.getGuestEmail(), r.getGuestPhone(), r.getCheckInDate(), r.getCheckOutDate(),
                        r.getNumberOfGuests(), r.getStatus(), r.getSource(), r.getTotalAmount(), r.getCurrency(),
                        r.getNotes(), r.getCreatedAt(),
                        messagesByReservation.getOrDefault(r.getId(), List.of()).stream()
                                .map(m -> new GdprMessageExport(m.getSenderType(), m.getBody(), m.getCreatedAt()))
                                .toList(),
                        lateCheckoutByReservation.getOrDefault(r.getId(), List.of()).stream()
                                .map(lc -> new GdprLateCheckoutExport(lc.getStatus(), lc.getGuestNote(), lc.getCreatedAt()))
                                .toList()))
                .toList();

        List<GdprLeadExport> leadExports = leads.stream()
                .map(l -> new GdprLeadExport(l.getId(), l.getFullName(), l.getEmail(), l.getPhone(), l.getCity(),
                        l.getMessage(), l.isContacted(), l.getCreatedAt()))
                .toList();

        int recordsAffected = reservationExports.size() + leadExports.size();
        recordComplianceEntryAndAuditAfterCommit(GdprRequestType.EXPORT, AuditAction.GDPR_DATA_EXPORTED,
                actor, email, verificationMethod, recordsAffected,
                "Export de date GDPR pentru %s (%d rezervări, %d lead-uri)"
                        .formatted(PiiMasking.maskEmail(email), reservationExports.size(), leadExports.size()));

        return new GdprExportResponse(email, Instant.now(), reservationExports, leadExports);
    }

    @Transactional
    public GdprEraseResultResponse erase(String email, GdprVerificationMethod verificationMethod,
                                          String verificationNote, User actor) {
        requireValidVerification(verificationMethod, verificationNote);

        List<Reservation> reservations = reservationRepository.findByGuestEmailIgnoreCase(email);
        List<PropertyLead> leads = propertyLeadRepository.findByEmailIgnoreCase(email);
        if (reservations.isEmpty() && leads.isEmpty()) {
            throw new BadRequestException("Nu s-au găsit înregistrări pentru acest email");
        }

        List<UUID> reservationIds = reservations.stream().map(Reservation::getId).toList();
        int messagesRedacted = reservationIds.isEmpty()
                ? 0
                : messageRepository.redactMessagesForReservations(reservationIds, REDACTED_MESSAGE);

        if (!reservationIds.isEmpty()) {
            List<String> linkPaths = reservationIds.stream()
                    .map(id -> "/dashboard/reservations/" + id)
                    .toList();
            notificationRepository.redactByLinkPaths(linkPaths, REDACTED_NOTIFICATION_TITLE, REDACTED_NOTIFICATION_BODY);
        }

        // Late checkout requests carry their own free-text guestNote,
        // submittable by the guest directly - not covered by clearing the
        // Reservation fields above.
        int lateCheckoutNotesRedacted = reservationIds.isEmpty()
                ? 0
                : lateCheckoutRequestRepository.redactGuestNoteForReservations(reservationIds);

        for (Reservation r : reservations) {
            r.setGuestFirstName("Șters");
            r.setGuestLastName("(GDPR)");
            r.setGuestEmail(null);
            r.setGuestPhone(null);
            r.setNotes(null);
            r.setAccessCode(null);
            // revoke the unauthenticated self-service link - otherwise anyone still
            // holding it can keep viewing, modifying, cancelling, or messaging about
            // a booking whose guest asked to no longer be tracked.
            r.setManagementToken(null);
        }
        reservationRepository.saveAll(reservations);

        for (PropertyLead lead : leads) {
            lead.setFullName("Șters (GDPR)");
            lead.setEmail(null);
            lead.setPhone(null);
            lead.setMessage(null);
        }
        propertyLeadRepository.saveAll(leads);

        int recordsAffected = reservations.size() + leads.size();
        recordComplianceEntryAndAuditAfterCommit(GdprRequestType.ERASE, AuditAction.GDPR_DATA_ERASED,
                actor, email, verificationMethod, recordsAffected,
                ("Date GDPR șterse pentru %s (%d rezervări anonimizate, %d lead-uri anonimizate, "
                        + "%d mesaje redactate, %d note de check-out târziu redactate)")
                        .formatted(PiiMasking.maskEmail(email), reservations.size(), leads.size(),
                                messagesRedacted, lateCheckoutNotesRedacted));

        return new GdprEraseResultResponse(
                reservations.size(), leads.size(), messagesRedacted, lateCheckoutNotesRedacted);
    }

    private void requireValidVerification(GdprVerificationMethod verificationMethod, String verificationNote) {
        if (verificationMethod == null) {
            throw new BadRequestException("Metoda de verificare a identității este obligatorie");
        }
        if (verificationNote == null || verificationNote.isBlank()) {
            throw new BadRequestException("Notița de verificare este obligatorie");
        }
        if (verificationNote.length() > MAX_VERIFICATION_NOTE_LENGTH) {
            throw new BadRequestException("Notița de verificare este prea lungă (max " + MAX_VERIFICATION_NOTE_LENGTH + " caractere)");
        }
        if (LOOKS_LIKE_ID_NUMBER.matcher(verificationNote).find()) {
            throw new BadRequestException(
                    "Notița de verificare pare să conțină un CNP sau un număr de act de identitate - "
                            + "nu introduce acte sau identificatori personali aici, doar cum a fost verificată identitatea");
        }
    }

    /**
     * Persists the compliance-register row inside this transaction (so it
     * rolls back together with the anonymization if anything fails), but
     * defers the {@link AuditService#record} call - which commits
     * independently via REQUIRES_NEW - until this transaction has actually
     * committed. Without the deferral, a failure between this call and the
     * method returning would leave an audit entry claiming success for an
     * operation that was, in fact, rolled back.
     */
    private void recordComplianceEntryAndAuditAfterCommit(GdprRequestType type, AuditAction auditAction, User actor,
                                                            String email, GdprVerificationMethod verificationMethod,
                                                            int recordsAffected, String auditDescription) {
        String maskedEmail = PiiMasking.maskEmail(email);
        String fingerprint = PiiMasking.fingerprintEmail(email, appProperties.getJwt().getSecret());

        gdprRequestRepository.save(GdprRequest.builder()
                .requestType(type)
                .actor(actor)
                .maskedEmail(maskedEmail)
                .emailFingerprint(fingerprint)
                .verificationMethod(verificationMethod)
                .verifiedAt(Instant.now())
                .recordsAffected(recordsAffected)
                .build());

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    auditService.record(auditAction, actor, auditDescription, null, null);
                }
            });
        } else {
            // no active transaction synchronization (e.g. called outside a
            // managed transaction in a test) - fall back to recording immediately.
            auditService.record(auditAction, actor, auditDescription, null, null);
        }
    }
}
