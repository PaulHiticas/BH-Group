package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.domain.MaintenanceTicket;
import com.bhgroup.pms.domain.MaintenanceTicketPhoto;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.repository.MaintenanceTicketPhotoRepository;
import com.bhgroup.pms.repository.MaintenanceTicketRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.service.mapper.MaintenanceTicketMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

/**
 * Covers the authenticated-download path for maintenance ticket photos:
 * photos are no longer served as public static files (see SecurityConfig),
 * so loadPhotoResource/loadMyPhotoResource are the only way to read one,
 * and they must enforce the same ownership isolation as the rest of the
 * technician-facing API - a technician who isn't assigned to the ticket
 * gets a 404, never the photo.
 */
@ExtendWith(MockitoExtension.class)
class MaintenanceTicketServiceTest {

    @Mock
    private MaintenanceTicketRepository maintenanceTicketRepository;
    @Mock
    private MaintenanceTicketPhotoRepository maintenanceTicketPhotoRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private EmailService emailService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private MaintenanceTicketMapper maintenanceTicketMapper;
    @Mock
    private Resource resource;

    private MaintenanceTicketService maintenanceTicketService;

    private UUID ticketId;
    private UUID photoId;
    private UUID assigneeId;
    private MaintenanceTicket ticket;
    private MaintenanceTicketPhoto photo;

    @BeforeEach
    void setUp() {
        maintenanceTicketService = new MaintenanceTicketService(
                maintenanceTicketRepository, maintenanceTicketPhotoRepository, propertyRepository,
                userRepository, fileStorageService, emailService, notificationService, maintenanceTicketMapper);

        ticketId = UUID.randomUUID();
        photoId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();

        User assignee = User.builder().build();
        assignee.setId(assigneeId);
        ticket = MaintenanceTicket.builder().build();
        ticket.setId(ticketId);
        ticket.setAssignedTo(assignee);

        photo = MaintenanceTicketPhoto.builder()
                .id(photoId)
                .maintenanceTicket(ticket)
                .fileKey("maintenance-tickets/" + ticketId + "/photo.jpg")
                .url("http://example.com/photo.jpg")
                .build();
    }

    @Test
    void loadPhotoResource_returnsResource_whenPhotoBelongsToTicket() {
        when(maintenanceTicketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(maintenanceTicketPhotoRepository.findById(photoId)).thenReturn(Optional.of(photo));
        when(fileStorageService.loadAsResource(photo.getFileKey())).thenReturn(resource);

        Resource result = maintenanceTicketService.loadPhotoResource(ticketId, photoId);

        assertThat(result).isSameAs(resource);
    }

    @Test
    void loadPhotoResource_throwsNotFound_whenPhotoBelongsToADifferentTicket() {
        MaintenanceTicket otherTicket = MaintenanceTicket.builder().build();
        otherTicket.setId(UUID.randomUUID());
        photo.setMaintenanceTicket(otherTicket);

        when(maintenanceTicketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(maintenanceTicketPhotoRepository.findById(photoId)).thenReturn(Optional.of(photo));

        assertThatThrownBy(() -> maintenanceTicketService.loadPhotoResource(ticketId, photoId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void loadMyPhotoResource_returnsResource_whenCallerIsTheAssignedTechnician() {
        when(maintenanceTicketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(maintenanceTicketPhotoRepository.findById(photoId)).thenReturn(Optional.of(photo));
        when(fileStorageService.loadAsResource(photo.getFileKey())).thenReturn(resource);

        Resource result = maintenanceTicketService.loadMyPhotoResource(assigneeId, ticketId, photoId);

        assertThat(result).isSameAs(resource);
    }

    @Test
    void loadMyPhotoResource_throwsNotFound_whenCallerIsNotTheAssignedTechnician() {
        UUID someoneElseId = UUID.randomUUID();
        when(maintenanceTicketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> maintenanceTicketService.loadMyPhotoResource(someoneElseId, ticketId, photoId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void loadMyPhotoResource_throwsNotFound_whenTicketHasNoAssignedTechnician() {
        ticket.setAssignedTo(null);
        when(maintenanceTicketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> maintenanceTicketService.loadMyPhotoResource(assigneeId, ticketId, photoId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
