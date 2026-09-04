package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.config.AppProperties;
import com.bhgroup.pms.domain.AssistantChat;
import com.bhgroup.pms.domain.AssistantChatMessage;
import com.bhgroup.pms.domain.AssistantChatSenderType;
import com.bhgroup.pms.domain.AssistantChatStatus;
import com.bhgroup.pms.domain.NotificationType;
import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.domain.UserStatus;
import com.bhgroup.pms.dto.assistant.AssistantChatDetailResponse;
import com.bhgroup.pms.dto.assistant.AssistantChatMessageResponse;
import com.bhgroup.pms.dto.assistant.AssistantChatReplyRequest;
import com.bhgroup.pms.dto.assistant.AssistantChatSummaryResponse;
import com.bhgroup.pms.dto.assistant.AssistantHandoffRequest;
import com.bhgroup.pms.dto.assistant.AssistantHandoffResponse;
import com.bhgroup.pms.dto.assistant.AssistantMessageRequest;
import com.bhgroup.pms.repository.AssistantChatMessageRepository;
import com.bhgroup.pms.repository.AssistantChatRepository;
import com.bhgroup.pms.repository.NotificationRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.security.SecureTokenGenerator;
import com.bhgroup.pms.service.mapper.AssistantChatMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI-assistant human handoff chats: mirrors {@link OwnerThreadService}'s
 * OPEN/RESOLVED lifecycle and staff-notification flow, but for an
 * anonymous website visitor instead of an authenticated owner. The
 * visitor is identified by an unguessable public token (same raw-token
 * approach as {@code Reservation.managementToken}), not a User account -
 * there's no in-app Notification to send them, so they read staff
 * replies by polling {@link #getMessagesByToken(String)} instead.
 *
 * <p>This is the single entry point for creating a handoff chat, used
 * both when a visitor explicitly asks for a human and when
 * {@link AssistantService} itself flags a reply as needing one - both
 * paths funnel into the same {@link #createHandoff(AssistantHandoffRequest)},
 * so there is exactly one place that notifies staff.
 */
@Service
@RequiredArgsConstructor
public class AssistantChatService {

    private static final List<Role> ADMIN_ROLES = List.of(Role.SUPER_ADMIN, Role.ADMINISTRATOR);

    private final AssistantChatRepository assistantChatRepository;
    private final AssistantChatMessageRepository assistantChatMessageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SecureTokenGenerator secureTokenGenerator;
    private final AssistantChatMapper assistantChatMapper;
    private final AppProperties appProperties;

    // ------------------------------------------------------------------
    // Visitor-facing (public, token-scoped)
    // ------------------------------------------------------------------

    @Transactional
    public AssistantHandoffResponse createHandoff(AssistantHandoffRequest request) {
        String token = secureTokenGenerator.generateRawToken();

        AssistantChat chat = AssistantChat.builder()
                .publicToken(token)
                .guestName(request.guestName())
                .guestEmail(request.guestEmail())
                .status(AssistantChatStatus.OPEN)
                .lastMessageAt(Instant.now())
                .build();
        chat = assistantChatRepository.save(chat);

        for (AssistantMessageRequest historyMessage : request.messages()) {
            AssistantChatMessage message = AssistantChatMessage.builder()
                    .chat(chat)
                    .senderType("assistant".equals(historyMessage.role())
                            ? AssistantChatSenderType.AI : AssistantChatSenderType.GUEST)
                    .body(historyMessage.content())
                    .build();
            assistantChatMessageRepository.save(message);
        }

        notifyAdmins(chat, request.messages());

        return new AssistantHandoffResponse(token);
    }

    @Transactional(readOnly = true)
    public List<AssistantChatMessageResponse> getMessagesByToken(String publicToken) {
        AssistantChat chat = assistantChatRepository.findByPublicToken(publicToken)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));
        return assistantChatMessageRepository.findByChatIdOrderByCreatedAtAsc(chat.getId()).stream()
                .map(assistantChatMapper::toMessageResponse)
                .toList();
    }

    /**
     * The in-app notification carries no visitor PII (mirrors LeadService's
     * reasoning) - staff get the actual guest name/preview only in the
     * alert email, sent directly to them.
     */
    private void notifyAdmins(AssistantChat chat, List<AssistantMessageRequest> history) {
        String guestLabel = chat.getGuestName() != null && !chat.getGuestName().isBlank()
                ? chat.getGuestName() : "Un vizitator";
        String preview = lastGuestMessagePreview(history);

        notificationService.notifyAdmins(
                NotificationType.NEW_ASSISTANT_HANDOFF,
                guestLabel + " cere să vorbească cu un coleg",
                "Conversație nouă din asistentul AI",
                "/dashboard/assistant-chats/" + chat.getId());

        for (User admin : userRepository.findByRoleInAndStatus(ADMIN_ROLES, UserStatus.ACTIVE)) {
            if (admin.getEmail() != null) {
                emailService.sendAssistantHandoffEmail(
                        admin.getEmail(), admin.getFirstName(), guestLabel, preview, chat.getId());
            }
        }
    }

    private String lastGuestMessagePreview(List<AssistantMessageRequest> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            if ("user".equals(history.get(i).role())) {
                return history.get(i).content();
            }
        }
        return history.isEmpty() ? "" : history.get(history.size() - 1).content();
    }

    // ------------------------------------------------------------------
    // Staff-facing (SUPER_ADMIN / ADMINISTRATOR)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<AssistantChatSummaryResponse> listForStaff(AssistantChatStatus status, Pageable pageable) {
        Page<AssistantChat> page = status != null
                ? assistantChatRepository.findByStatus(status, sortedByLastMessageDesc(pageable))
                : assistantChatRepository.findAll(sortedByLastMessageDesc(pageable));
        return PageResponse.of(page, assistantChatMapper::toSummaryResponse);
    }

    @Transactional
    public AssistantChatDetailResponse getForStaff(UUID chatId) {
        AssistantChat chat = requireChat(chatId);
        assistantChatMessageRepository.markMessagesReadForSenderType(
                chatId, AssistantChatSenderType.GUEST, Instant.now());
        List<AssistantChatMessage> messages = assistantChatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        return assistantChatMapper.toDetailResponse(chat, messages);
    }

    @Transactional
    public AssistantChatMessageResponse addStaffMessage(UUID chatId, UUID staffUserId,
                                                          AssistantChatReplyRequest request) {
        AssistantChat chat = requireChat(chatId);
        User staff = userRepository.findById(staffUserId).orElse(null);

        AssistantChatMessage message = AssistantChatMessage.builder()
                .chat(chat)
                .senderType(AssistantChatSenderType.STAFF)
                .senderUser(staff)
                .body(request.body())
                .build();
        message = assistantChatMessageRepository.save(message);

        chat.setLastMessageAt(Instant.now());
        assistantChatRepository.save(chat);

        return assistantChatMapper.toMessageResponse(message);
    }

    @Transactional
    public void resolve(UUID chatId) {
        AssistantChat chat = requireChat(chatId);
        chat.setStatus(AssistantChatStatus.RESOLVED);
        assistantChatRepository.save(chat);
    }

    private AssistantChat requireChat(UUID chatId) {
        return assistantChatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));
    }

    private Pageable sortedByLastMessageDesc(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "lastMessageAt"));
    }

    // ------------------------------------------------------------------
    // Retention (GDPR storage limitation - see AssistantChatRetentionScheduler)
    // ------------------------------------------------------------------

    /**
     * Deletes every chat (OPEN or RESOLVED) whose last activity predates
     * {@code app.assistant.retention-days} - a single, activity-based cutoff
     * rather than a per-status rule, so a recently-active OPEN chat is never
     * touched but one that's simply gone stale for that long is treated the
     * same as an old RESOLVED one. Chat rows are deleted outright (their
     * messages cascade via the DB FK, see V38) rather than anonymized:
     * unlike a Reservation, nothing outside this feature depends on a
     * purged chat's row still existing. The admin notifications that
     * announced each purged chat (see {@link #notifyAdmins}) are deleted
     * too - otherwise the guest's name embedded in their title would
     * outlive the chat it came from.
     */
    @Transactional
    public int purgeOldChats() {
        Instant cutoff = Instant.now().minus(appProperties.getAssistant().getRetentionDays(), ChronoUnit.DAYS);
        List<AssistantChat> stale = assistantChatRepository.findByLastMessageAtBefore(cutoff);
        if (stale.isEmpty()) {
            return 0;
        }

        List<String> linkPaths = stale.stream().map(c -> "/dashboard/assistant-chats/" + c.getId()).toList();
        notificationRepository.deleteByLinkPathIn(linkPaths);
        assistantChatRepository.deleteAll(stale);
        return stale.size();
    }
}
