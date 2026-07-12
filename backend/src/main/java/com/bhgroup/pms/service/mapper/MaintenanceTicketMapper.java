package com.bhgroup.pms.service.mapper;

import com.bhgroup.pms.domain.MaintenanceTicket;
import com.bhgroup.pms.domain.MaintenanceTicketPhoto;
import com.bhgroup.pms.dto.maintenance.MaintenanceTicketPhotoResponse;
import com.bhgroup.pms.dto.maintenance.MaintenanceTicketResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceTicketMapper {

    public MaintenanceTicketResponse toResponse(MaintenanceTicket ticket, List<MaintenanceTicketPhoto> photos) {
        return new MaintenanceTicketResponse(
                ticket.getId(),
                ticket.getProperty().getId(),
                ticket.getProperty().getName(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getReportedBy() != null ? ticket.getReportedBy().getId() : null,
                ticket.getReportedBy() != null
                        ? ticket.getReportedBy().getFirstName() + " " + ticket.getReportedBy().getLastName() : null,
                ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null,
                ticket.getAssignedTo() != null
                        ? ticket.getAssignedTo().getFirstName() + " " + ticket.getAssignedTo().getLastName() : null,
                ticket.getVendor(),
                ticket.getEstimatedCost(),
                ticket.getActualCost(),
                ticket.getResolvedAt(),
                photos.stream().map(this::toPhotoResponse).toList(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }

    public MaintenanceTicketPhotoResponse toPhotoResponse(MaintenanceTicketPhoto photo) {
        return new MaintenanceTicketPhotoResponse(
                photo.getId(), photo.getUrl(), photo.getCaption(), photo.getCreatedAt());
    }
}
