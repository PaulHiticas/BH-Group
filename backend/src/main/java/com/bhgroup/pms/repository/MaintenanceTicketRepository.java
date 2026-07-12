package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.MaintenancePriority;
import com.bhgroup.pms.domain.MaintenanceStatus;
import com.bhgroup.pms.domain.MaintenanceTicket;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, UUID>,
        JpaSpecificationExecutor<MaintenanceTicket> {

    List<MaintenanceTicket> findByPropertyIdAndPriorityAndStatusNotIn(
            UUID propertyId, MaintenancePriority priority, List<MaintenanceStatus> excludedStatuses);
}
