package com.bhstays.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bhstays.pms.common.exception.ResourceNotFoundException;
import com.bhstays.pms.config.AppProperties;
import com.bhstays.pms.domain.AssistantChat;
import com.bhstays.pms.domain.AssistantChatMessage;
import com.bhstays.pms.domain.AssistantChatSenderType;
import com.bhstays.pms.domain.AssistantChatStatus;
import com.bhstays.pms.domain.NotificationType;
import com.bhstays.pms.domain.Role;
import com.bhstays.pms.domain.User;
import com.bhstays.pms.domain.UserStatus;
import com.bhstays.pms.dto.assistant.AssistantChatReplyRequest;
import com.bhstays.pms.dto.assistant.AssistantHandoffRequest;
import com.bhstays.pms.dto.assistant.AssistantHandoffResponse;
import com.bhstays.pms.dto.assistant.AssistantMessageRequest;
import com.bhstays.pms.repository.AssistantChatMessageRepository;
import com.bhstays.pms.repository.AssistantChatRepository;
import com.bhstays.pms.repository.NotificationRepository;
import com.bhstays.pms.repository.UserRepository;
import com.bhstays.pms.security.SecureTokenGenerator;
import com.bhstays.pms.service.mapper.AssistantChatMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AssistantChatServiceTest {

    @Mock
    private AssistantChatRepository assistantChatRepository;
    @Mock
    private AssistantChatMessageRepository assistantChatMessageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private SecureTokenGenerator secureTokenGenerator;

    private AssistantChatService assistantChatService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getAssistant().setRetentionDays(90);

        assistantChatService = new AssistantChatService(
                assistantChatRepository, assistantChatMessageRepository, userRepository,
                notificationService, notificationRepository, emailService, secureTokenGenerator,
                new AssistantChatMapper(), appProperties);
    }

    private User activeAdmin(String email, Role role) {
        User admin = User.builder().email(email).firstName("Admin").lastName("User")
                .role(role).status(UserStatus.ACTIVE).build();
        admin.setId(UUID.randomUUID());
        return admin;
    }

    @Test
    void createHandoff_savesChatAndMessages_notifiesAdmins_emailsEachActiveAdmin() {
        when(secureTokenGenerator.generateRawToken()).thenReturn("raw-token-123");
        when(assistantChatRepository.save(any(AssistantChat.class))).thenAnswer(invocation -> {
            AssistantChat chat = invocation.getArgument(0);
            chat.setId(UUID.randomUUID());
            return chat;
        });
        User denis = activeAdmin("denis@bhstays.ro", Role.ADMINISTRATOR);
        User you = activeAdmin("you@bhstays.ro", Role.SUPER_ADMIN);
        when(userRepository.findByRoleInAndStatus(List.of(Role.SUPER_ADMIN, Role.ADMINISTRATOR), UserStatus.ACTIVE))
                .thenReturn(List.of(denis, you));

        AssistantHandoffRequest request = new AssistantHandoffRequest(
                List.of(
                        new AssistantMessageRequest("user", "Vreau să vorbesc cu cineva"),
                        new AssistantMessageRequest("assistant", "Un coleg te va contacta.")),
                "Ion Popescu", "ion@example.com");

        AssistantHandoffResponse response = assistantChatService.createHandoff(request);

        assertThat(response.publicToken()).isEqualTo("raw-token-123");
        verify(assistantChatMessageRepository, times(2)).save(any(AssistantChatMessage.class));
        verify(notificationService).notifyAdmins(
                eq(NotificationType.NEW_ASSISTANT_HANDOFF), anyString(), anyString(), anyString());
        verify(emailService).sendAssistantHandoffEmail(
                eq("denis@bhstays.ro"), eq("Admin"), eq("Ion Popescu"), anyString(), any(UUID.class));
        verify(emailService).sendAssistantHandoffEmail(
                eq("you@bhstays.ro"), eq("Admin"), eq("Ion Popescu"), anyString(), any(UUID.class));
    }

    @Test
    void createHandoff_mapsMessageRolesToGuestAndAi() {
        when(secureTokenGenerator.generateRawToken()).thenReturn("raw-token-123");
        when(assistantChatRepository.save(any(AssistantChat.class))).thenAnswer(invocation -> {
            AssistantChat chat = invocation.getArgument(0);
            chat.setId(UUID.randomUUID());
            return chat;
        });
        when(userRepository.findByRoleInAndStatus(any(), eq(UserStatus.ACTIVE))).thenReturn(List.of());

        AssistantHandoffRequest request = new AssistantHandoffRequest(
                List.of(
                        new AssistantMessageRequest("user", "Bună"),
                        new AssistantMessageRequest("assistant", "Salut, cu ce te pot ajuta?")),
                null, null);

        assistantChatService.createHandoff(request);

        ArgumentCaptor<AssistantChatMessage> captor = ArgumentCaptor.forClass(AssistantChatMessage.class);
        verify(assistantChatMessageRepository, times(2)).save(captor.capture());
        List<AssistantChatMessage> saved = captor.getAllValues();
        assertThat(saved.get(0).getSenderType()).isEqualTo(AssistantChatSenderType.GUEST);
        assertThat(saved.get(1).getSenderType()).isEqualTo(AssistantChatSenderType.AI);
    }

    @Test
    void getMessagesByToken_returnsMessagesInOrder() {
        AssistantChat chat = AssistantChat.builder().publicToken("tok-1").build();
        chat.setId(UUID.randomUUID());
        when(assistantChatRepository.findByPublicToken("tok-1")).thenReturn(Optional.of(chat));

        AssistantChatMessage m1 = AssistantChatMessage.builder().chat(chat)
                .senderType(AssistantChatSenderType.GUEST).body("Salut").build();
        AssistantChatMessage m2 = AssistantChatMessage.builder().chat(chat)
                .senderType(AssistantChatSenderType.STAFF).body("Bună, cu ce te ajut?").build();
        when(assistantChatMessageRepository.findByChatIdOrderByCreatedAtAsc(chat.getId()))
                .thenReturn(List.of(m1, m2));

        var messages = assistantChatService.getMessagesByToken("tok-1");

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).body()).isEqualTo("Salut");
        assertThat(messages.get(1).senderType()).isEqualTo("STAFF");
    }

    @Test
    void getMessagesByToken_throwsNotFound_whenTokenIsUnknown() {
        when(assistantChatRepository.findByPublicToken("wrong-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assistantChatService.getMessagesByToken("wrong-token"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addStaffMessage_savesMessageAndUpdatesLastMessageAt() {
        AssistantChat chat = AssistantChat.builder().publicToken("tok-1").status(AssistantChatStatus.OPEN).build();
        chat.setId(UUID.randomUUID());
        when(assistantChatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        when(assistantChatMessageRepository.save(any(AssistantChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        UUID staffId = UUID.randomUUID();
        when(userRepository.findById(staffId)).thenReturn(Optional.empty());

        var response = assistantChatService.addStaffMessage(
                chat.getId(), staffId, new AssistantChatReplyRequest("Bună, cu ce te pot ajuta?"));

        assertThat(response.senderType()).isEqualTo("STAFF");
        assertThat(response.body()).isEqualTo("Bună, cu ce te pot ajuta?");
        verify(assistantChatRepository).save(chat);
    }

    @Test
    void getForStaff_marksGuestMessagesRead() {
        AssistantChat chat = AssistantChat.builder().publicToken("tok-1").build();
        chat.setId(UUID.randomUUID());
        when(assistantChatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        when(assistantChatMessageRepository.findByChatIdOrderByCreatedAtAsc(chat.getId())).thenReturn(List.of());

        assistantChatService.getForStaff(chat.getId());

        verify(assistantChatMessageRepository)
                .markMessagesReadForSenderType(eq(chat.getId()), eq(AssistantChatSenderType.GUEST), any());
    }

    @Test
    void resolve_setsStatusToResolved() {
        AssistantChat chat = AssistantChat.builder().publicToken("tok-1").status(AssistantChatStatus.OPEN).build();
        chat.setId(UUID.randomUUID());
        when(assistantChatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        when(assistantChatRepository.save(any(AssistantChat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assistantChatService.resolve(chat.getId());

        assertThat(chat.getStatus()).isEqualTo(AssistantChatStatus.RESOLVED);
    }

    @Test
    void listForStaff_filtersByStatusWhenProvided() {
        when(assistantChatRepository.findByStatus(eq(AssistantChatStatus.OPEN), any()))
                .thenReturn(Page.empty());

        assistantChatService.listForStaff(AssistantChatStatus.OPEN, PageRequest.of(0, 10));

        verify(assistantChatRepository).findByStatus(eq(AssistantChatStatus.OPEN), any());
        verify(assistantChatRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void purgeOldChats_deletesStaleChatsAndTheirNotifications() {
        AssistantChat stale = AssistantChat.builder().publicToken("tok-old").status(AssistantChatStatus.RESOLVED).build();
        stale.setId(UUID.randomUUID());
        when(assistantChatRepository.findByLastMessageAtBefore(any(Instant.class))).thenReturn(List.of(stale));

        int purged = assistantChatService.purgeOldChats();

        assertThat(purged).isEqualTo(1);
        verify(notificationRepository).deleteByLinkPathIn(List.of("/dashboard/assistant-chats/" + stale.getId()));
        verify(assistantChatRepository).deleteAll(List.of(stale));
    }

    @Test
    void purgeOldChats_doesNothingWhenNoChatIsStale() {
        when(assistantChatRepository.findByLastMessageAtBefore(any(Instant.class))).thenReturn(List.of());

        int purged = assistantChatService.purgeOldChats();

        assertThat(purged).isEqualTo(0);
        verify(notificationRepository, never()).deleteByLinkPathIn(any());
        verify(assistantChatRepository, never()).deleteAll(any());
    }
}
