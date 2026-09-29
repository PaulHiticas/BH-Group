package com.bhstays.pms.repository;

import com.bhstays.pms.domain.MaintenancePriority;
import com.bhstays.pms.domain.MaintenanceStatus;
import com.bhstays.pms.domain.MaintenanceTicket;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, UUID>,
        JpaSpecificationExecutor<MaintenanceTicket> {

    List<MaintenanceTicket> findByPropertyIdAndPriorityAndStatusNotIn(
            UUID propertyId, MaintenancePriority priority, List<MaintenanceStatus> excludedStatuses);
}
