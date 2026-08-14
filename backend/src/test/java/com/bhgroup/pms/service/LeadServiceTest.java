package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.domain.LeadType;
import com.bhgroup.pms.domain.NotificationType;
import com.bhgroup.pms.domain.PropertyLead;
import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.domain.UserStatus;
import com.bhgroup.pms.dto.lead.LeadCreateRequest;
import com.bhgroup.pms.repository.PropertyLeadRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.service.mapper.LeadMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private PropertyLeadRepository leadRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private LeadService leadService;

    @BeforeEach
    void setUp() {
        leadService = new LeadService(
                leadRepository, userRepository, emailService, notificationService, new LeadMapper(),
                transactionManager);
    }

    @Test
    void create_persistsAndNotifiesAdminsForARealSubmission() {
        User admin = new User();
        admin.setEmail("admin@bhgroup.io");
        admin.setFirstName("Admin");
        when(userRepository.findByRoleInAndStatus(
                eq(List.of(Role.SUPER_ADMIN, Role.ADMINISTRATOR)), eq(UserStatus.ACTIVE)))
                .thenReturn(List.of(admin));
        when(leadRepository.save(any(PropertyLead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeadCreateRequest request = new LeadCreateRequest(
                "Ana Popescu", "ana@example.com", null, "Cluj-Napoca", null,
                LeadType.REVENUE_ESTIMATE, 2, true, "google", "cpc", "spring", "");

        var response = leadService.create(request);

        assertThat(response.leadType()).isEqualTo(LeadType.REVENUE_ESTIMATE);
        assertThat(response.bedrooms()).isEqualTo(2);
        verify(leadRepository, times(1)).save(any(PropertyLead.class));
        verify(notificationService).notifyAdmins(
                eq(NotificationType.NEW_LEAD), any(), any(), eq("/dashboard/leads"));
        verify(emailService).sendNewLeadAlertEmail(
                eq("admin@bhgroup.io"), eq("Admin"), eq("Ana Popescu"), eq("ana@example.com"), any(), any());
    }

    @Test
    void create_inAppNotificationCarriesNoPii() {
        when(leadRepository.save(any(PropertyLead.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByRoleInAndStatus(any(), any())).thenReturn(List.of());

        LeadCreateRequest request = new LeadCreateRequest(
                "Ana Popescu", "ana@example.com", null, "Cluj-Napoca", null,
                LeadType.REVENUE_ESTIMATE, 2, true, null, null, null, "");

        leadService.create(request);

        // The in-app notification has no reservation-style link to redact on
        // GDPR erasure, so it must never carry the lead's name/city/message
        // in the first place - unlike the alert email, which goes straight
        // to staff and isn't stored in the app's own database.
        org.mockito.ArgumentCaptor<String> bodyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyAdmins(eq(NotificationType.NEW_LEAD), any(), bodyCaptor.capture(), any());
        assertThat(bodyCaptor.getValue()).doesNotContain("Ana Popescu").doesNotContain("Cluj-Napoca");
    }

    @Test
    void create_discardsSilentlyWhenHoneypotIsFilled() {
        LeadCreateRequest request = new LeadCreateRequest(
                "Bot", "bot@example.com", null, null, null,
                LeadType.GENERAL, null, true, null, null, null, "https://spam.example.com");

        var response = leadService.create(request);

        // Looks like success to the caller, but nothing was actually persisted or notified.
        assertThat(response.fullName()).isEqualTo("Bot");
        verify(leadRepository, never()).save(any(PropertyLead.class));
        verify(notificationService, never()).notifyAdmins(any(), any(), any(), any());
        verify(emailService, never()).sendNewLeadAlertEmail(any(), any(), any(), any(), any(), any());
    }

    @Test
    void create_defaultsToGeneralLeadTypeWhenNotSpecified() {
        when(leadRepository.save(any(PropertyLead.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByRoleInAndStatus(any(), any())).thenReturn(List.of());

        LeadCreateRequest request = new LeadCreateRequest(
                "Ion Vasile", "ion@example.com", null, null, null,
                null, null, true, null, null, null, null);

        var response = leadService.create(request);

        assertThat(response.leadType()).isEqualTo(LeadType.GENERAL);
    }
}
