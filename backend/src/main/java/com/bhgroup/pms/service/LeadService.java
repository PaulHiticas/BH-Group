package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.LeadType;
import com.bhgroup.pms.domain.NotificationType;
import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.domain.UserStatus;
import com.bhgroup.pms.dto.lead.LeadCreateRequest;
import com.bhgroup.pms.dto.lead.LeadResponse;
import com.bhgroup.pms.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.bhgroup.pms.domain.PropertyLead;
import com.bhgroup.pms.repository.PropertyLeadRepository;
import com.bhgroup.pms.service.mapper.LeadMapper;
@Slf4j
@Service
public class LeadService {

    private static final List<Role> ADMIN_ROLES = List.of(Role.SUPER_ADMIN, Role.ADMINISTRATOR);

    private final PropertyLeadRepository leadRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final LeadMapper leadMapper;
    private final TransactionTemplate requiresNewTransactionTemplate;

    public LeadService(PropertyLeadRepository leadRepository, UserRepository userRepository,
                        EmailService emailService, NotificationService notificationService,
                        LeadMapper leadMapper, PlatformTransactionManager transactionManager) {
        this.leadRepository = leadRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.leadMapper = leadMapper;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Persist-first-then-notify: notification (in-app and email) is
     * deferred to run only after this transaction actually commits, via
     * registerSynchronization().afterCommit() - calling notifyAdmins()
     * directly here would still run inside this transaction, before the
     * lead row is durably committed, so a commit failure (or an @Async
     * email thread racing ahead of the commit) could notify staff about a
     * lead that was never actually saved.
     *
     * afterCommit() runs after the physical commit but before this
     * transaction's resources are fully unbound from the thread, which
     * makes plain @Transactional(REQUIRED) on the notification path
     * unreliable at that exact point (confirmed by a real Testcontainers
     * run: the insert silently found no admins instead of throwing).
     * requiresNewTransactionTemplate forces a genuinely new, independent
     * transaction/connection for the notification write, scoped to just
     * this call site rather than changing NotificationService's
     * propagation for its other (non-afterCommit) callers.
     */
    @Transactional
    public LeadResponse create(LeadCreateRequest request) {
        if (request.website() != null && !request.website().isBlank()) {
            // Honeypot triggered: pretend success so the bot doesn't learn
            // it was caught, but never persist or notify about it.
            log.debug("Discarded honeypot-triggered lead submission from {}", maskEmail(request.email()));
            return fakeSuccessResponse(request);
        }

        PropertyLead lead = PropertyLead.builder()
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .city(request.city())
                .message(request.message())
                .leadType(request.leadType() != null ? request.leadType() : LeadType.GENERAL)
                .bedrooms(request.bedrooms())
                .consentGiven(request.consentGiven())
                .utmSource(request.utmSource())
                .utmMedium(request.utmMedium())
                .utmCampaign(request.utmCampaign())
                .build();

        lead = leadRepository.save(lead);
        log.info("New {} lead received from {}", lead.getLeadType(), maskEmail(lead.getEmail()));

        PropertyLead savedLead = lead;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    requiresNewTransactionTemplate.executeWithoutResult(status -> notifyAdmins(savedLead));
                }
            });
        } else {
            // No active transaction to defer to (e.g. called outside the
            // normal @Transactional proxy, such as directly in a unit
            // test) - notify immediately rather than silently dropping it.
            notifyAdmins(savedLead);
        }

        return leadMapper.toResponse(lead);
    }

    /**
     * The in-app notification deliberately carries no PII (name, city,
     * message) - it's a permanent row in the notifications table with no
     * link back to a specific lead id to redact later, so anything written
     * here would survive that lead being deleted. Staff get the actual
     * details (which do need to be real) only in the alert email, sent
     * directly to them, not stored in the app's own database.
     */
    private void notifyAdmins(PropertyLead lead) {
        String leadTypeLabel = lead.getLeadType() == LeadType.REVENUE_ESTIMATE
                ? "Cerere estimare venit" : "Lead nou";

        notificationService.notifyAdmins(NotificationType.NEW_LEAD, leadTypeLabel,
                "Un lead nou așteaptă să fie contactat", "/dashboard/leads");

        for (User admin : userRepository.findByRoleInAndStatus(ADMIN_ROLES, UserStatus.ACTIVE)) {
            if (admin.getEmail() != null) {
                emailService.sendNewLeadAlertEmail(
                        admin.getEmail(), admin.getFirstName(), lead.getFullName(),
                        lead.getEmail(), lead.getPhone(), leadTypeLabel);
            }
        }
    }

    private LeadResponse fakeSuccessResponse(LeadCreateRequest request) {
        return new LeadResponse(
                UUID.randomUUID(), request.fullName(), request.email(), request.phone(), request.city(),
                request.message(), false, request.leadType() != null ? request.leadType() : LeadType.GENERAL,
                request.bedrooms(), request.consentGiven(), request.utmSource(), request.utmMedium(),
                request.utmCampaign(), Instant.now());
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String maskedLocal = local.length() <= 1 ? "*" : local.charAt(0) + "***";
        return maskedLocal + domain;
    }

    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> list(Pageable pageable) {
        return PageResponse.of(leadRepository.findAllByOrderByCreatedAtDesc(pageable), leadMapper::toResponse);
    }

    @Transactional
    public LeadResponse markContacted(UUID id, boolean contacted) {
        PropertyLead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
        lead.setContacted(contacted);
        lead = leadRepository.save(lead);
        return leadMapper.toResponse(lead);
    }

    @Transactional(readOnly = true)
    public long countUncontacted() {
        return leadRepository.countByContactedFalse();
    }

    @Transactional(readOnly = true)
    public List<List<String>> exportRows() {
        return leadRepository.findAll(Sort.by("createdAt").descending())
                .stream()
                .map(lead -> List.of(
                        lead.getFullName(),
                        lead.getEmail(),
                        lead.getPhone() != null ? lead.getPhone() : "",
                        lead.getCity() != null ? lead.getCity() : "",
                        lead.getMessage() != null ? lead.getMessage() : "",
                        lead.isContacted() ? "Da" : "Nu",
                        lead.getLeadType().name(),
                        lead.getCreatedAt().toString()
                ))
                .toList();
    }
}
