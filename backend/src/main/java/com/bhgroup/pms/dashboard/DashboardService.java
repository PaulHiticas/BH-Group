package com.bhgroup.pms.dashboard;

import com.bhgroup.pms.dashboard.dto.DashboardSummaryResponse;
import com.bhgroup.pms.lead.LeadMapper;
import com.bhgroup.pms.lead.PropertyLeadRepository;
import com.bhgroup.pms.property.PropertyRepository;
import com.bhgroup.pms.reservation.ReservationMapper;
import com.bhgroup.pms.reservation.ReservationRepository;
import com.bhgroup.pms.reservation.ReservationStatus;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PropertyRepository propertyRepository;
    private final ReservationRepository reservationRepository;
    private final PropertyLeadRepository leadRepository;
    private final ReservationMapper reservationMapper;
    private final LeadMapper leadMapper;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        long totalProperties = propertyRepository.count();
        long totalReservations = reservationRepository.count();
        var totalRevenue = reservationRepository.sumTotalRevenue(ReservationStatus.NON_BLOCKING);
        long uncontactedLeads = leadRepository.countByContactedFalse();

        var upcomingReservations = reservationRepository
                .findUpcoming(LocalDate.now(), ReservationStatus.NON_BLOCKING, PageRequest.of(0, 6))
                .stream()
                .map(reservationMapper::toResponse)
                .toList();

        var recentLeads = leadRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 6))
                .stream()
                .map(leadMapper::toResponse)
                .toList();

        return new DashboardSummaryResponse(
                totalProperties, totalReservations, totalRevenue, "RON",
                uncontactedLeads, upcomingReservations, recentLeads);
    }
}
