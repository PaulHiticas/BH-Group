package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.NotificationType;
import com.bhgroup.pms.domain.OwnerThread;
import com.bhgroup.pms.domain.OwnerThreadMessage;
import com.bhgroup.pms.domain.OwnerThreadSenderType;
import com.bhgroup.pms.domain.OwnerThreadStatus;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.dto.ownerthread.OwnerThreadCreateRequest;
import com.bhgroup.pms.dto.ownerthread.OwnerThreadDetailResponse;
import com.bhgroup.pms.dto.ownerthread.OwnerThreadMessageCreateRequest;
import com.bhgroup.pms.dto.ownerthread.OwnerThreadMessageResponse;
import com.bhgroup.pms.dto.ownerthread.OwnerThreadSummaryResponse;
import com.bhgroup.pms.repository.OwnerThreadMessageRepository;
import com.bhgroup.pms.repository.OwnerThreadRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.service.mapper.OwnerThreadMapper;
import java.time.Instant;
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
 * Owner-to-admin contact threads: a parallel, independent conversation
 * model from the per-reservation guest/staff messaging in
 * {@link MessageService} — different audience and lifecycle (threads can
 * be resolved and reopened).
 *
 * <p>Owner-facing methods enforce strict per-owner isolation, the same
 * way {@link OwnerService} does: any thread id that doesn't belong to
 * the calling owner resolves as "not found" (404), never 403 — an owner
 * must not be able to distinguish "someone else's thread" from
 * "no such thread" (IDOR).
 */
@Service
@RequiredArgsConstructor
public class OwnerThreadService {

    private final OwnerThreadRepository ownerThreadRepository;
    private final OwnerThreadMessageRepository ownerThreadMessageRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final OwnerThreadMapper ownerThreadMapper;

    // ------------------------------------------------------------------
    // Owner-facing
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<OwnerThreadSummaryResponse> listForOwner(UUID ownerId, Pageable pageable) {
        Page<OwnerThread> page = ownerThreadRepository.findByOwnerId(ownerId, sortedByLastMessageDesc(pageable));
        return PageResponse.of(page, ownerThreadMapper::toSummaryResponse);
    }

    @Transactional
    public OwnerThreadDetailResponse createForOwner(UUID ownerId, OwnerThreadCreateRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Property property = null;
        if (request.propertyId() != null) {
            property = propertyRepository.findById(request.propertyId())
                    .filter(p -> p.getOwner() != null && p.getOwner().getId().equals(ownerId))
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        }

        OwnerThread thread = OwnerThread.builder()
                .owner(owner)
                .property(property)
                .subject(request.subject())
                .status(OwnerThreadStatus.OPEN)
                .lastMessageAt(Instant.now())
                .build();
        thread = ownerThreadRepository.save(thread);

        OwnerThreadMessage message = OwnerThreadMessage.builder()
                .thread(thread)
                .senderType(OwnerThreadSenderType.OWNER)
                .senderUser(owner)
                .body(request.body())
                .build();
        message = ownerThreadMessageRepository.save(message);

        notificationService.notifyAdmins(
                NotificationType.NEW_OWNER_REQUEST,
                "Cerere nouă de la " + owner.getFullName(),
                request.subject(),
                "/dashboard/owner-requests/" + thread.getId());

        return ownerThreadMapper.toDetailResponse(thread, List.of(message));
    }

    @Transactional
    public OwnerThreadDetailResponse getForOwner(UUID ownerId, UUID threadId) {
        OwnerThread thread = requireOwnerThread(ownerId, threadId);
        ownerThreadMessageRepository.markThreadReadForViewer(threadId, OwnerThreadSenderType.OWNER, Instant.now());
        List<OwnerThreadMessage> messages = ownerThreadMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId);
        return ownerThreadMapper.toDetailResponse(thread, messages);
    }

    @Transactional
    public OwnerThreadMessageResponse addOwnerMessage(UUID ownerId, UUID threadId,
                                                        OwnerThreadMessageCreateRequest request) {
        OwnerThread thread = requireOwnerThread(ownerId, threadId);

        OwnerThreadMessage message = OwnerThreadMessage.builder()
                .thread(thread)
                .senderType(OwnerThreadSenderType.OWNER)
                .senderUser(thread.getOwner())
                .body(request.body())
                .build();
        message = ownerThreadMessageRepository.save(message);

        thread.setLastMessageAt(Instant.now());
        boolean reopened = thread.getStatus() == OwnerThreadStatus.RESOLVED;
        if (reopened) {
            thread.setStatus(OwnerThreadStatus.OPEN);
        }
        ownerThreadRepository.save(thread);

        if (reopened) {
            notificationService.notifyAdmins(
                    NotificationType.NEW_OWNER_REQUEST,
                    "Cerere redeschisă de " + thread.getOwner().getFullName(),
                    request.body(),
                    "/dashboard/owner-requests/" + thread.getId());
        }

        return ownerThreadMapper.toMessageResponse(message);
    }

    private OwnerThread requireOwnerThread(UUID ownerId, UUID threadId) {
        return ownerThreadRepository.findById(threadId)
                .filter(t -> t.getOwner().getId().equals(ownerId))
                .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
    }

    // ------------------------------------------------------------------
    // Staff-facing (SUPER_ADMIN / ADMINISTRATOR / SUPPORT_AGENT)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<OwnerThreadSummaryResponse> listForStaff(OwnerThreadStatus status, Pageable pageable) {
        Page<OwnerThread> page = status != null
                ? ownerThreadRepository.findByStatus(status, sortedByLastMessageDesc(pageable))
                : ownerThreadRepository.findAll(sortedByLastMessageDesc(pageable));
        return PageResponse.of(page, ownerThreadMapper::toSummaryResponse);
    }

    @Transactional
    public OwnerThreadDetailResponse getForStaff(UUID threadId) {
        OwnerThread thread = requireThread(threadId);
        ownerThreadMessageRepository.markThreadReadForViewer(threadId, OwnerThreadSenderType.STAFF, Instant.now());
        List<OwnerThreadMessage> messages = ownerThreadMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId);
        return ownerThreadMapper.toDetailResponse(thread, messages);
    }

    @Transactional
    public OwnerThreadMessageResponse addStaffMessage(UUID threadId, UUID staffUserId,
                                                        OwnerThreadMessageCreateRequest request) {
        OwnerThread thread = requireThread(threadId);
        User staff = userRepository.findById(staffUserId).orElse(null);

        OwnerThreadMessage message = OwnerThreadMessage.builder()
                .thread(thread)
                .senderType(OwnerThreadSenderType.STAFF)
                .senderUser(staff)
                .body(request.body())
                .build();
        message = ownerThreadMessageRepository.save(message);

        thread.setLastMessageAt(Instant.now());
        ownerThreadRepository.save(thread);

        notificationService.notifyUser(
                thread.getOwner().getId(),
                NotificationType.OWNER_REQUEST_REPLY,
                "Răspuns nou pentru cererea „" + thread.getSubject() + "”",
                request.body(),
                "/dashboard/owner/threads/" + thread.getId());

        return ownerThreadMapper.toMessageResponse(message);
    }

    @Transactional
    public void resolve(UUID threadId) {
        OwnerThread thread = requireThread(threadId);
        thread.setStatus(OwnerThreadStatus.RESOLVED);
        ownerThreadRepository.save(thread);
    }

    private OwnerThread requireThread(UUID threadId) {
        return ownerThreadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
    }

    private Pageable sortedByLastMessageDesc(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "lastMessageAt"));
    }
}
