package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.domain.AuditAction;
import com.bhgroup.pms.domain.GdprRecordType;
import com.bhgroup.pms.domain.Message;
import com.bhgroup.pms.domain.PropertyLead;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.dto.gdpr.GdprEraseResultResponse;
import com.bhgroup.pms.dto.gdpr.GdprExportResponse;
import com.bhgroup.pms.dto.gdpr.GdprLeadExport;
import com.bhgroup.pms.dto.gdpr.GdprMessageExport;
import com.bhgroup.pms.dto.gdpr.GdprReservationExport;
import com.bhgroup.pms.dto.gdpr.GdprSearchMatchResponse;
import com.bhgroup.pms.repository.MessageRepository;
import com.bhgroup.pms.repository.PropertyLeadRepository;
import com.bhgroup.pms.repository.ReservationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 */
@Service
@RequiredArgsConstructor
public class GdprService {

    private static final String REDACTED_MESSAGE = "[mesaj șters - solicitare GDPR]";

    private final ReservationRepository reservationRepository;
    private final PropertyLeadRepository propertyLeadRepository;
    private final MessageRepository messageRepository;
    private final AuditService auditService;

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

    @Transactional(readOnly = true)
    public GdprExportResponse export(String email, User actor) {
        List<Reservation> reservations = reservationRepository.findByGuestEmailIgnoreCase(email);
        List<PropertyLead> leads = propertyLeadRepository.findByEmailIgnoreCase(email);
        if (reservations.isEmpty() && leads.isEmpty()) {
            throw new BadRequestException("Nu s-au găsit înregistrări pentru acest email");
        }

        Map<UUID, List<Message>> messagesByReservation = reservations.isEmpty()
                ? Map.of()
                : messageRepository.findByReservationIdInOrderByCreatedAtAsc(
                        reservations.stream().map(Reservation::getId).toList())
                    .stream().collect(Collectors.groupingBy(m -> m.getReservation().getId()));

        List<GdprReservationExport> reservationExports = reservations.stream()
                .map(r -> new GdprReservationExport(
                        r.getId(), r.getProperty().getName(), r.getGuestFirstName(), r.getGuestLastName(),
                        r.getGuestEmail(), r.getGuestPhone(), r.getCheckInDate(), r.getCheckOutDate(),
                        r.getNumberOfGuests(), r.getStatus(), r.getSource(), r.getTotalAmount(), r.getCurrency(),
                        r.getNotes(), r.getCreatedAt(),
                        messagesByReservation.getOrDefault(r.getId(), List.of()).stream()
                                .map(m -> new GdprMessageExport(m.getSenderType(), m.getBody(), m.getCreatedAt()))
                                .toList()))
                .toList();

        List<GdprLeadExport> leadExports = leads.stream()
                .map(l -> new GdprLeadExport(l.getId(), l.getFullName(), l.getEmail(), l.getPhone(), l.getCity(),
                        l.getMessage(), l.isContacted(), l.getCreatedAt()))
                .toList();

        auditService.record(AuditAction.GDPR_DATA_EXPORTED, actor,
                "Export de date GDPR pentru " + email + " (" + reservationExports.size() + " rezervări, "
                        + leadExports.size() + " lead-uri)",
                null, null);

        return new GdprExportResponse(email, Instant.now(), reservationExports, leadExports);
    }

    @Transactional
    public GdprEraseResultResponse erase(String email, User actor) {
        List<Reservation> reservations = reservationRepository.findByGuestEmailIgnoreCase(email);
        List<PropertyLead> leads = propertyLeadRepository.findByEmailIgnoreCase(email);
        if (reservations.isEmpty() && leads.isEmpty()) {
            throw new BadRequestException("Nu s-au găsit înregistrări pentru acest email");
        }

        List<UUID> reservationIds = reservations.stream().map(Reservation::getId).toList();
        int messagesRedacted = reservationIds.isEmpty()
                ? 0
                : messageRepository.redactGuestMessagesForReservations(reservationIds, REDACTED_MESSAGE);

        for (Reservation r : reservations) {
            r.setGuestFirstName("Șters");
            r.setGuestLastName("(GDPR)");
            r.setGuestEmail(null);
            r.setGuestPhone(null);
            r.setNotes(null);
            r.setAccessCode(null);
        }
        reservationRepository.saveAll(reservations);

        for (PropertyLead lead : leads) {
            lead.setFullName("Șters (GDPR)");
            lead.setEmail(null);
            lead.setPhone(null);
            lead.setMessage(null);
        }
        propertyLeadRepository.saveAll(leads);

        auditService.record(AuditAction.GDPR_DATA_ERASED, actor,
                "Date GDPR șterse pentru " + email + " (" + reservations.size() + " rezervări anonimizate, "
                        + leads.size() + " lead-uri anonimizate, " + messagesRedacted + " mesaje redactate)",
                null, null);

        return new GdprEraseResultResponse(reservations.size(), leads.size(), messagesRedacted);
    }
}
