package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.domain.NotificationType;
import com.bhgroup.pms.domain.OwnerThread;
import com.bhgroup.pms.domain.OwnerThreadMessage;
import com.bhgroup.pms.domain.OwnerThreadSenderType;
import com.bhgroup.pms.domain.OwnerThreadStatus;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.dto.ownerthread.OwnerThreadCreateRequest;
import com.bhgroup.pms.dto.ownerthread.OwnerThreadDetailResponse;
import com.bhgroup.pms.dto.ownerthread.OwnerThreadMessageCreateRequest;
import com.bhgroup.pms.dto.ownerthread.OwnerThreadMessageResponse;
import com.bhgroup.pms.repository.OwnerThreadMessageRepository;
import com.bhgroup.pms.repository.OwnerThreadRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.service.mapper.OwnerThreadMapper;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OwnerThreadServiceTest {

    @Mock
    private OwnerThreadRepository ownerThreadRepository;
    @Mock
    private OwnerThreadMessageRepository ownerThreadMessageRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    private OwnerThreadService ownerThreadService;

    private User owner;
    private User otherOwner;
    private User staff;

    @BeforeEach
    void setUp() {
        ownerThreadService = new OwnerThreadService(
                ownerThreadRepository, ownerThreadMessageRepository, propertyRepository, userRepository,
                notificationService, new OwnerThreadMapper());

        owner = User.builder().firstName("Ana").lastName("Popescu").role(Role.OWNER).build();
        owner.setId(UUID.randomUUID());

        otherOwner = User.builder().firstName("Ion").lastName("Ionescu").role(Role.OWNER).build();
        otherOwner.setId(UUID.randomUUID());

        staff = User.builder().firstName("Maria").lastName("Suport").role(Role.ADMINISTRATOR).build();
        staff.setId(UUID.randomUUID());
    }

    // ------------------------------------------------------------------
    // Owner isolation (IDOR -> 404)
    // ------------------------------------------------------------------

    @Test
    void getForOwner_threadBelongsToAnotherOwner_throwsNotFound() {
        OwnerThread thread = thread(otherOwner, null, OwnerThreadStatus.OPEN);
        when(ownerThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));

        assertThatThrownBy(() -> ownerThreadService.getForOwner(owner.getId(), thread.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addOwnerMessage_threadBelongsToAnotherOwner_throwsNotFound() {
        OwnerThread thread = thread(otherOwner, null, OwnerThreadStatus.OPEN);
        when(ownerThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));

        assertThatThrownBy(() -> ownerThreadService.addOwnerMessage(
                owner.getId(), thread.getId(), new OwnerThreadMessageCreateRequest("Salut")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getForOwner_ownThread_succeedsAndMarksOnlyStaffMessagesRead() {
        OwnerThread thread = thread(owner, null, OwnerThreadStatus.OPEN);
        when(ownerThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));
        when(ownerThreadMessageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId())).thenReturn(List.of());

        OwnerThreadDetailResponse response = ownerThreadService.getForOwner(owner.getId(), thread.getId());

        assertThat(response.id()).isEqualTo(thread.getId());
        verify(ownerThreadMessageRepository)
                .markThreadReadForViewer(eq(thread.getId()), eq(OwnerThreadSenderType.OWNER), any());
    }

    @Test
    void createForOwner_propertyBelongsToAnotherOwner_throwsNotFound() {
        Property foreignProperty = property(otherOwner);
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(propertyRepository.findById(foreignProperty.getId())).thenReturn(Optional.of(foreignProperty));

        OwnerThreadCreateRequest request = new OwnerThreadCreateRequest("Întrebare", foreignProperty.getId(), "Mesaj");

        assertThatThrownBy(() -> ownerThreadService.createForOwner(owner.getId(), request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createForOwner_ownProperty_succeedsAndNotifiesAdmins() {
        Property ownProperty = property(owner);
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(propertyRepository.findById(ownProperty.getId())).thenReturn(Optional.of(ownProperty));
        when(ownerThreadRepository.save(any())).thenAnswer(invocation -> {
            OwnerThread saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId(UUID.randomUUID());
            return saved;
        });
        when(ownerThreadMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OwnerThreadCreateRequest request = new OwnerThreadCreateRequest("Reparație boiler", ownProperty.getId(), "Boilerul nu funcționează");
        OwnerThreadDetailResponse response = ownerThreadService.createForOwner(owner.getId(), request);

        assertThat(response.subject()).isEqualTo("Reparație boiler");
        assertThat(response.status()).isEqualTo(OwnerThreadStatus.OPEN);
        assertThat(response.messages()).hasSize(1);
        verify(notificationService).notifyAdmins(eq(NotificationType.NEW_OWNER_REQUEST), anyString(), anyString(), anyString());
    }

    @Test
    void createForOwner_withoutProperty_isAGeneralRequest() {
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(ownerThreadRepository.save(any())).thenAnswer(invocation -> {
            OwnerThread saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId(UUID.randomUUID());
            return saved;
        });
        when(ownerThreadMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OwnerThreadCreateRequest request = new OwnerThreadCreateRequest("Întrebare generală", null, "Cum funcționează statementele?");
        OwnerThreadDetailResponse response = ownerThreadService.createForOwner(owner.getId(), request);

        assertThat(response.propertyId()).isNull();
        verify(propertyRepository, never()).findById(any());
    }

    // ------------------------------------------------------------------
    // Lifecycle: RESOLVED -> reopen on owner reply
    // ------------------------------------------------------------------

    @Test
    void addOwnerMessage_onResolvedThread_reopensAndNotifiesAdmins() {
        OwnerThread thread = thread(owner, null, OwnerThreadStatus.RESOLVED);
        when(ownerThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));
        when(ownerThreadMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ownerThreadService.addOwnerMessage(owner.getId(), thread.getId(), new OwnerThreadMessageCreateRequest("Mai am o problemă"));

        assertThat(thread.getStatus()).isEqualTo(OwnerThreadStatus.OPEN);
        verify(notificationService).notifyAdmins(eq(NotificationType.NEW_OWNER_REQUEST), anyString(), anyString(), anyString());
    }

    @Test
    void addOwnerMessage_onOpenThread_doesNotNotifyAdminsAndUpdatesLastMessageAt() {
        OwnerThread thread = thread(owner, null, OwnerThreadStatus.OPEN);
        Instant before = thread.getLastMessageAt();
        when(ownerThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));
        when(ownerThreadMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ownerThreadService.addOwnerMessage(owner.getId(), thread.getId(), new OwnerThreadMessageCreateRequest("Reply normal"));

        assertThat(thread.getStatus()).isEqualTo(OwnerThreadStatus.OPEN);
        assertThat(thread.getLastMessageAt()).isAfterOrEqualTo(before);
        verify(notificationService, never()).notifyAdmins(any(), anyString(), anyString(), anyString());
    }

    // ------------------------------------------------------------------
    // Staff side: sees everything, replies notify the owner, can resolve
    // ------------------------------------------------------------------

    @Test
    void listForStaff_noStatusFilter_listsEveryThread() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<OwnerThread> page = new PageImpl<>(List.of(thread(owner, null, OwnerThreadStatus.OPEN)));
        when(ownerThreadRepository.findAll(any(Pageable.class))).thenReturn(page);

        ownerThreadService.listForStaff(null, pageable);

        verify(ownerThreadRepository).findAll(any(Pageable.class));
        verify(ownerThreadRepository, never()).findByStatus(any(), any());
    }

    @Test
    void listForStaff_withStatusFilter_filtersByStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<OwnerThread> page = new PageImpl<>(List.of());
        when(ownerThreadRepository.findByStatus(eq(OwnerThreadStatus.RESOLVED), any(Pageable.class))).thenReturn(page);

        ownerThreadService.listForStaff(OwnerThreadStatus.RESOLVED, pageable);

        verify(ownerThreadRepository).findByStatus(eq(OwnerThreadStatus.RESOLVED), any(Pageable.class));
    }

    @Test
    void getForStaff_marksOnlyOwnerMessagesRead() {
        OwnerThread thread = thread(owner, null, OwnerThreadStatus.OPEN);
        when(ownerThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));
        when(ownerThreadMessageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId())).thenReturn(List.of());

        ownerThreadService.getForStaff(thread.getId());

        verify(ownerThreadMessageRepository)
                .markThreadReadForViewer(eq(thread.getId()), eq(OwnerThreadSenderType.STAFF), any());
    }

    @Test
    void addStaffMessage_notifiesOnlyTheThreadOwner() {
        OwnerThread thread = thread(owner, null, OwnerThreadStatus.OPEN);
        when(ownerThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(ownerThreadMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OwnerThreadMessageResponse response = ownerThreadService.addStaffMessage(
                thread.getId(), staff.getId(), new OwnerThreadMessageCreateRequest("Rezolvăm imediat"));

        assertThat(response.senderType()).isEqualTo(OwnerThreadSenderType.STAFF);
        ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(notificationService).notifyUser(
                userIdCaptor.capture(), eq(NotificationType.OWNER_REQUEST_REPLY), anyString(), anyString(), anyString());
        assertThat(userIdCaptor.getValue()).isEqualTo(owner.getId());
    }

    @Test
    void resolve_setsStatusToResolved() {
        OwnerThread thread = thread(owner, null, OwnerThreadStatus.OPEN);
        when(ownerThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));

        ownerThreadService.resolve(thread.getId());

        assertThat(thread.getStatus()).isEqualTo(OwnerThreadStatus.RESOLVED);
        verify(ownerThreadRepository).save(thread);
    }

    @Test
    void newMessage_neverArtificiallyMarksItselfRead() {
        OwnerThread thread = thread(owner, null, OwnerThreadStatus.OPEN);
        when(ownerThreadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        ArgumentCaptor<OwnerThreadMessage> messageCaptor = ArgumentCaptor.forClass(OwnerThreadMessage.class);
        when(ownerThreadMessageRepository.save(messageCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        ownerThreadService.addStaffMessage(thread.getId(), staff.getId(), new OwnerThreadMessageCreateRequest("Salut"));

        assertThat(messageCaptor.getValue().getReadAt()).isNull();
    }

    private OwnerThread thread(User threadOwner, Property property, OwnerThreadStatus status) {
        OwnerThread thread = OwnerThread.builder()
                .owner(threadOwner)
                .property(property)
                .subject("Test subject")
                .status(status)
                .lastMessageAt(Instant.now().minusSeconds(60))
                .build();
        thread.setId(UUID.randomUUID());
        return thread;
    }

    private Property property(User propertyOwner) {
        Property property = Property.builder().name("Test Apartment").owner(propertyOwner).build();
        property.setId(UUID.randomUUID());
        return property;
    }
}
