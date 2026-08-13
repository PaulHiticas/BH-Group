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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhgroup.pms.domain.PropertyLead;
import com.bhgroup.pms.repository.PropertyLeadRepository;
import com.bhgroup.pms.service.mapper.LeadMapper;
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private static final List<Role> ADMIN_ROLES = List.of(Role.SUPER_ADMIN, Role.ADMINISTRATOR);

    private final PropertyLeadRepository leadRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final LeadMapper leadMapper;

    /**
     * Persist-first-then-notify: the lead is saved before any notification
     * is attempted, and notifications (especially email) never roll back or
     * block the response if they fail - see EmailService, whose send()
     * methods are @Async and catch their own exceptions.
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

        notifyAdmins(lead);

        return leadMapper.toResponse(lead);
    }

    private void notifyAdmins(PropertyLead lead) {
        String leadTypeLabel = lead.getLeadType() == LeadType.REVENUE_ESTIMATE
                ? "Cerere estimare venit" : "Lead nou";

        notificationService.notifyAdmins(NotificationType.NEW_LEAD, leadTypeLabel,
                lead.getFullName() + (lead.getCity() != null ? " — " + lead.getCity() : ""),
                "/dashboard/leads");

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
