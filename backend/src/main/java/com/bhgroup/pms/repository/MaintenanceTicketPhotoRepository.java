package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.MaintenanceTicketPhoto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceTicketPhotoRepository extends JpaRepository<MaintenanceTicketPhoto, UUID> {

    List<MaintenanceTicketPhoto> findByMaintenanceTicketIdOrderByCreatedAtAsc(UUID maintenanceTicketId);
}
