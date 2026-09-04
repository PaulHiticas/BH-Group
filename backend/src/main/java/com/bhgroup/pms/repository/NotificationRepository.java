package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Notification> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(UUID userId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.user.id = :userId and n.readAt is null")
    void markAllReadForUser(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Notifications aren't linked to a Reservation by a foreign key - they're
     * free-standing per-user rows with the guest's name baked into
     * {@code title} and the message body copied into {@code body} at
     * creation time (see MessageService.sendGuestMessage). GDPR erasure
     * matches them back to a reservation via the same {@code linkPath} the
     * notification was created with ("/dashboard/reservations/{id}"), since
     * that's the only thread tying the two together.
     */
    @Modifying
    @Query("""
            update Notification n set n.title = :redactedTitle, n.body = :redactedBody
            where n.linkPath in :linkPaths
            """)
    int redactByLinkPaths(@Param("linkPaths") List<String> linkPaths,
                           @Param("redactedTitle") String redactedTitle,
                           @Param("redactedBody") String redactedBody);

    /**
     * Retention (not a data-subject request): once the assistant chats
     * behind these notifications have themselves been purged, the
     * notification copies of the guest's name/preview (see
     * AssistantChatService#notifyAdmins) have nothing left to reference -
     * delete them outright rather than leaving them to redact later.
     */
    @Modifying
    @Query("delete from Notification n where n.linkPath in :linkPaths")
    int deleteByLinkPathIn(@Param("linkPaths") List<String> linkPaths);
}
