package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.MaintenanceCategory;
import com.bhgroup.pms.domain.MaintenancePriority;
import com.bhgroup.pms.domain.MaintenanceStatus;
import com.bhgroup.pms.domain.MaintenanceTicket;
import com.bhgroup.pms.domain.MaintenanceTicketPhoto;
import com.bhgroup.pms.domain.NotificationType;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.dto.maintenance.MaintenanceTicketAssignRequest;
import com.bhgroup.pms.dto.maintenance.MaintenanceTicketCreateRequest;
import com.bhgroup.pms.dto.maintenance.MaintenanceTicketResponse;
import com.bhgroup.pms.dto.maintenance.MaintenanceTicketStatusUpdateRequest;
import com.bhgroup.pms.dto.maintenance.MaintenanceTicketUpdateRequest;
import com.bhgroup.pms.repository.MaintenanceTicketPhotoRepository;
import com.bhgroup.pms.repository.MaintenanceTicketRepository;
import com.bhgroup.pms.repository.MaintenanceTicketSpecifications;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.service.mapper.MaintenanceTicketMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MaintenanceTicketService {

    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final MaintenanceTicketPhotoRepository maintenanceTicketPhotoRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final MaintenanceTicketMapper maintenanceTicketMapper;

    @Transactional(readOnly = true)
    public PageResponse<MaintenanceTicketResponse> list(UUID propertyId, MaintenanceStatus status,
                                                          MaintenancePriority priority, MaintenanceCategory category,
                                                          UUID assigneeId, Pageable pageable) {
        Specification<MaintenanceTicket> spec = MaintenanceTicketSpecifications.combine(
                MaintenanceTicketSpecifications.hasProperty(propertyId),
                MaintenanceTicketSpecifications.hasStatus(status),
                MaintenanceTicketSpecifications.hasPriority(priority),
                MaintenanceTicketSpecifications.hasCategory(category),
                MaintenanceTicketSpecifications.hasAssignee(assigneeId)
        );
        Page<MaintenanceTicket> page = maintenanceTicketRepository.findAll(spec, pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<MaintenanceTicketResponse> listMine(UUID assigneeId, MaintenanceStatus status,
                                                              Pageable pageable) {
        return list(null, status, null, null, assigneeId, pageable);
    }

    @Transactional(readOnly = true)
    public MaintenanceTicketResponse get(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public MaintenanceTicketResponse getMine(UUID assigneeId, UUID id) {
        MaintenanceTicket ticket = findOrThrow(id);
        assertAssignedTo(ticket, assigneeId);
        return toResponse(ticket);
    }

    @Transactional
    public MaintenanceTicketResponse create(MaintenanceTicketCreateRequest request, UUID reportedByUserId) {
        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        MaintenanceTicket ticket = MaintenanceTicket.builder()
                .property(property)
                .title(request.title())
                .description(request.description())
                .category(request.category() != null ? request.category() : MaintenanceCategory.OTHER)
                .priority(request.priority() != null ? request.priority() : MaintenancePriority.MEDIUM)
                .status(MaintenanceStatus.OPEN)
                .vendor(request.vendor())
                .estimatedCost(request.estimatedCost())
                .build();

        if (reportedByUserId != null) {
            userRepository.findById(reportedByUserId).ifPresent(ticket::setReportedBy);
        }

        ticket = maintenanceTicketRepository.save(ticket);

        if (ticket.getPriority() == MaintenancePriority.CRITICAL) {
            blockPropertyAndNotifyOwner(property, ticket);
        }

        return toResponse(ticket);
    }

    @Transactional
    public MaintenanceTicketResponse update(UUID id, MaintenanceTicketUpdateRequest request) {
        MaintenanceTicket ticket = findOrThrow(id);
        boolean becameCritical = ticket.getPriority() != MaintenancePriority.CRITICAL
                && request.priority() == MaintenancePriority.CRITICAL;

        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        if (request.category() != null) ticket.setCategory(request.category());
        if (request.priority() != null) ticket.setPriority(request.priority());
        ticket.setVendor(request.vendor());
        ticket.setEstimatedCost(request.estimatedCost());
        ticket.setActualCost(request.actualCost());

        ticket = maintenanceTicketRepository.save(ticket);

        if (becameCritical) {
            blockPropertyAndNotifyOwner(ticket.getProperty(), ticket);
        }

        return toResponse(ticket);
    }

    @Transactional
    public MaintenanceTicketResponse assign(UUID id, MaintenanceTicketAssignRequest request) {
        MaintenanceTicket ticket = findOrThrow(id);
        User assignee = userRepository.findById(request.assignedToId())
                .orElseThrow(() -> new BadRequestException("Assignee not found"));
        if (assignee.getRole() != Role.MAINTENANCE) {
            throw new BadRequestException("Assigned user must have the MAINTENANCE role");
        }
        ticket.setAssignedTo(assignee);
        if (ticket.getStatus() == MaintenanceStatus.OPEN) {
            ticket.setStatus(MaintenanceStatus.IN_PROGRESS);
        }
        ticket = maintenanceTicketRepository.save(ticket);
        return toResponse(ticket);
    }

    @Transactional
    public MaintenanceTicketResponse updateStatus(UUID id, MaintenanceTicketStatusUpdateRequest request) {
        return toResponse(applyStatusChange(findOrThrow(id), request.status()));
    }

    @Transactional
    public MaintenanceTicketResponse updateMyStatus(UUID assigneeId, UUID id,
                                                      MaintenanceTicketStatusUpdateRequest request) {
        MaintenanceTicket ticket = findOrThrow(id);
        assertAssignedTo(ticket, assigneeId);
        return toResponse(applyStatusChange(ticket, request.status()));
    }

    @Transactional
    public MaintenanceTicketResponse addPhoto(UUID id, MultipartFile file, String caption) {
        return addPhotoInternal(findOrThrow(id), file, caption);
    }

    @Transactional
    public MaintenanceTicketResponse addMyPhoto(UUID assigneeId, UUID id, MultipartFile file, String caption) {
        MaintenanceTicket ticket = findOrThrow(id);
        assertAssignedTo(ticket, assigneeId);
        return addPhotoInternal(ticket, file, caption);
    }

    private MaintenanceTicketResponse addPhotoInternal(MaintenanceTicket ticket, MultipartFile file, String caption) {
        FileStorageService.StoredFile stored = fileStorageService.storeImage(
                file, "maintenance-tickets/" + ticket.getId());
        MaintenanceTicketPhoto photo = MaintenanceTicketPhoto.builder()
                .maintenanceTicket(ticket)
                .fileKey(stored.fileKey())
                .url(stored.url())
                .caption(caption)
                .build();
        maintenanceTicketPhotoRepository.save(photo);
        return toResponse(ticket);
    }

    private MaintenanceTicket applyStatusChange(MaintenanceTicket ticket, MaintenanceStatus target) {
        ticket.setStatus(target);
        if (target == MaintenanceStatus.RESOLVED || target == MaintenanceStatus.CLOSED) {
            ticket.setResolvedAt(Instant.now());
            unblockPropertyIfNoOtherCriticalTickets(ticket);
        }
        return maintenanceTicketRepository.save(ticket);
    }

    private void blockPropertyAndNotifyOwner(Property property, MaintenanceTicket ticket) {
        if (property.getStatus() == PropertyStatus.ACTIVE) {
            property.setStatus(PropertyStatus.MAINTENANCE);
            propertyRepository.save(property);
        }
        if (property.getOwner() != null && property.getOwner().getEmail() != null) {
            emailService.sendMaintenanceAlertEmail(
                    property.getOwner().getEmail(), property.getOwner().getFirstName(),
                    property.getName(), ticket.getTitle(), ticket.getDescription());
        }
        notificationService.notifyAdmins(
                NotificationType.CRITICAL_MAINTENANCE,
                "Problemă critică la " + property.getName(),
                ticket.getTitle(),
                "/dashboard/maintenance/" + ticket.getId());
    }

    /** Restores a critically-blocked property to ACTIVE once no other open/in-progress critical tickets remain. */
    private void unblockPropertyIfNoOtherCriticalTickets(MaintenanceTicket resolvedTicket) {
        Property property = resolvedTicket.getProperty();
        if (property.getStatus() != PropertyStatus.MAINTENANCE) {
            return;
        }
        List<MaintenanceTicket> stillCritical = maintenanceTicketRepository
                .findByPropertyIdAndPriorityAndStatusNotIn(
                        property.getId(), MaintenancePriority.CRITICAL,
                        List.of(MaintenanceStatus.RESOLVED, MaintenanceStatus.CLOSED));
        if (stillCritical.isEmpty()) {
            property.setStatus(PropertyStatus.ACTIVE);
            propertyRepository.save(property);
        }
    }

    private void assertAssignedTo(MaintenanceTicket ticket, UUID assigneeId) {
        // 404 rather than 403: a technician should not learn that a ticket
        // exists when it isn't assigned to them.
        if (ticket.getAssignedTo() == null || !ticket.getAssignedTo().getId().equals(assigneeId)) {
            throw new ResourceNotFoundException("Maintenance ticket not found");
        }
    }

    private MaintenanceTicketResponse toResponse(MaintenanceTicket ticket) {
        var photos = maintenanceTicketPhotoRepository.findByMaintenanceTicketIdOrderByCreatedAtAsc(ticket.getId());
        return maintenanceTicketMapper.toResponse(ticket, photos);
    }

    private MaintenanceTicket findOrThrow(UUID id) {
        return maintenanceTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance ticket not found"));
    }
}
