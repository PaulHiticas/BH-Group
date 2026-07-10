package com.bhgroup.pms.service;

import com.bhgroup.pms.dto.dashboard.DashboardSummaryResponse;
import com.bhgroup.pms.service.mapper.LeadMapper;
import com.bhgroup.pms.repository.PropertyLeadRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.service.mapper.ReservationMapper;
import com.bhgroup.pms.repository.ReservationRepository;
import com.bhgroup.pms.domain.ReservationStatus;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhgroup.pms.domain.ReservationStatus;
import com.bhgroup.pms.dto.dashboard.DashboardSummaryResponse;
import com.bhgroup.pms.repository.PropertyLeadRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.ReservationRepository;
import com.bhgroup.pms.service.mapper.LeadMapper;
import com.bhgroup.pms.service.mapper.ReservationMapper;
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
