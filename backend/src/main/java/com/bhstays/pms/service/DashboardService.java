package com.bhstays.pms.service;

import com.bhstays.pms.dto.dashboard.DashboardSummaryResponse;
import com.bhstays.pms.service.mapper.LeadMapper;
import com.bhstays.pms.repository.PropertyLeadRepository;
import com.bhstays.pms.repository.PropertyRepository;
import com.bhstays.pms.service.mapper.ReservationMapper;
import com.bhstays.pms.repository.ReservationRepository;
import com.bhstays.pms.domain.ReservationStatus;
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
