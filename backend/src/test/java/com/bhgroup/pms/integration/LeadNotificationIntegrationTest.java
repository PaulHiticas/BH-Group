package com.bhgroup.pms.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.bhgroup.pms.domain.LeadType;
import com.bhgroup.pms.domain.Notification;
import com.bhgroup.pms.domain.PropertyLead;
import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.domain.UserStatus;
import com.bhgroup.pms.dto.lead.LeadCreateRequest;
import com.bhgroup.pms.repository.PropertyLeadRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.service.LeadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/**
 * A mocked-repository unit test can't prove the notification actually
 * fires only after the lead's insert has genuinely committed - only a real
 * transaction against a real database can. This runs LeadService.create()
 * through its real @Transactional boundary (no test-level rollback wrapper
 * here, see AbstractIntegrationTest), so by the time the call returns, the
 * afterCommit() callback has necessarily already run.
 */
class LeadNotificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LeadService leadService;
    @Autowired
    private PropertyLeadRepository leadRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private com.bhgroup.pms.repository.NotificationRepository notificationRepository;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(User.builder()
                .email("admin-" + System.nanoTime() + "@bhgroup.io")
                .passwordHash("irrelevant-for-this-test")
                .firstName("Admin")
                .lastName("Test")
                .role(Role.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
    }

    @Test
    void leadIsCommittedAndNotificationExistsWithNoPiiAfterCreateReturns() {
        LeadCreateRequest request = new LeadCreateRequest(
                "Ana Popescu", "ana-" + System.nanoTime() + "@example.com", null, "Cluj-Napoca", null,
                LeadType.REVENUE_ESTIMATE, 2, true, "google", "cpc", "spring", "");

        var response = leadService.create(request);

        // The lead itself is genuinely committed (not just returned in memory).
        PropertyLead persisted = leadRepository.findById(response.id()).orElseThrow();
        assertThat(persisted.getFullName()).isEqualTo("Ana Popescu");

        // The notification exists (proving afterCommit() actually ran) and
        // carries no PII from the lead.
        var notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(admin.getId(), PageRequest.of(0, 10))
                .getContent();
        assertThat(notifications).isNotEmpty();
        Notification notification = notifications.get(0);
        assertThat(notification.getBody()).doesNotContain("Ana Popescu").doesNotContain("Cluj-Napoca");
        assertThat(notification.getLinkPath()).isEqualTo("/dashboard/leads");
    }
}
