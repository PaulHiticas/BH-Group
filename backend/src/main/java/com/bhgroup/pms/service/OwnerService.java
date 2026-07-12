package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.Expense;
import com.bhgroup.pms.domain.MaintenanceStatus;
import com.bhgroup.pms.domain.MaintenanceTicket;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.domain.ReservationStatus;
import com.bhgroup.pms.dto.expense.ExpenseResponse;
import com.bhgroup.pms.dto.maintenance.MaintenanceTicketResponse;
import com.bhgroup.pms.dto.owner.OwnerDashboardSummaryResponse;
import com.bhgroup.pms.dto.owner.OwnerPropertyResponse;
import com.bhgroup.pms.dto.reservation.ReservationResponse;
import com.bhgroup.pms.repository.ExpenseRepository;
import com.bhgroup.pms.repository.ExpenseSpecifications;
import com.bhgroup.pms.repository.MaintenanceTicketPhotoRepository;
import com.bhgroup.pms.repository.MaintenanceTicketRepository;
import com.bhgroup.pms.repository.MaintenanceTicketSpecifications;
import com.bhgroup.pms.repository.PropertyDocumentRepository;
import com.bhgroup.pms.repository.PropertyPhotoRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.ReservationRepository;
import com.bhgroup.pms.repository.ReservationSpecifications;
import com.bhgroup.pms.service.mapper.ExpenseMapper;
import com.bhgroup.pms.service.mapper.MaintenanceTicketMapper;
import com.bhgroup.pms.service.mapper.OwnerMapper;
import com.bhgroup.pms.service.mapper.ReservationMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owner-scoped reads. Every query here is filtered by the authenticated
 * owner's id — an owner must never be able to see another owner's
 * properties, reservations, or revenue, including by manipulating a
 * property/reservation id in the URL (IDOR).
 */
@Service
@RequiredArgsConstructor
public class OwnerService {

    private final PropertyRepository propertyRepository;
    private final PropertyPhotoRepository propertyPhotoRepository;
    private final PropertyDocumentRepository propertyDocumentRepository;
    private final ReservationRepository reservationRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final MaintenanceTicketPhotoRepository maintenanceTicketPhotoRepository;
    private final ExpenseRepository expenseRepository;
    private final OwnerMapper ownerMapper;
    private final ReservationMapper reservationMapper;
    private final MaintenanceTicketMapper maintenanceTicketMapper;
    private final ExpenseMapper expenseMapper;

    @Transactional(readOnly = true)
    public PageResponse<OwnerPropertyResponse> listMyProperties(UUID ownerId, Pageable pageable) {
        Page<Property> page = propertyRepository.findByOwnerId(ownerId, pageable);
        return PageResponse.of(page, this::toOwnerPropertyResponse);
    }

    @Transactional(readOnly = true)
    public OwnerPropertyResponse getMyProperty(UUID ownerId, UUID propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .filter(p -> p.getOwner() != null && p.getOwner().getId().equals(ownerId))
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        return toOwnerPropertyResponse(property);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> listMyReservations(UUID ownerId, ReservationStatus status,
                                                                  LocalDate from, LocalDate to, Pageable pageable) {
        Specification<Reservation> spec = ReservationSpecifications.combine(
                ReservationSpecifications.hasPropertyOwner(ownerId),
                ReservationSpecifications.hasStatus(status),
                ReservationSpecifications.checkInFrom(from),
                ReservationSpecifications.checkInTo(to)
        );
        Page<Reservation> page = reservationRepository.findAll(spec, pageable);
        return PageResponse.of(page, reservationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OwnerDashboardSummaryResponse getMyDashboardSummary(UUID ownerId) {
        var properties = propertyRepository.findByOwnerId(ownerId);

        BigDecimal grossRevenue = BigDecimal.ZERO;
        BigDecimal commissionAmount = BigDecimal.ZERO;
        BigDecimal expensesTotal = BigDecimal.ZERO;
        for (Property property : properties) {
            BigDecimal propertyRevenue = reservationRepository
                    .sumRevenueForProperty(property.getId(), ReservationStatus.NON_BLOCKING);
            grossRevenue = grossRevenue.add(propertyRevenue);
            if (property.getCommissionPercent() != null) {
                commissionAmount = commissionAmount.add(propertyRevenue
                        .multiply(property.getCommissionPercent())
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP));
            }
            expensesTotal = expensesTotal.add(
                    expenseRepository.sumChargeableToOwnerForProperty(property.getId(), null, null));
        }
        BigDecimal netRevenue = grossRevenue.subtract(commissionAmount).subtract(expensesTotal);

        Specification<Reservation> upcomingSpec = ReservationSpecifications.combine(
                ReservationSpecifications.hasPropertyOwner(ownerId),
                ReservationSpecifications.checkInFrom(LocalDate.now()),
                ReservationSpecifications.activeOnly()
        );
        var upcoming = reservationRepository
                .findAll(upcomingSpec, PageRequest.of(0, 6, org.springframework.data.domain.Sort.by("checkInDate")))
                .stream()
                .map(reservationMapper::toResponse)
                .toList();

        Specification<MaintenanceTicket> openTicketsSpec = MaintenanceTicketSpecifications.combine(
                MaintenanceTicketSpecifications.hasPropertyOwner(ownerId),
                MaintenanceTicketSpecifications.statusNotIn(List.of(MaintenanceStatus.RESOLVED, MaintenanceStatus.CLOSED))
        );
        var openTickets = maintenanceTicketRepository
                .findAll(openTicketsSpec, PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").descending()))
                .stream()
                .map(this::toMaintenanceTicketResponse)
                .toList();

        return new OwnerDashboardSummaryResponse(
                properties.size(), grossRevenue, commissionAmount, expensesTotal, netRevenue, "RON",
                upcoming, openTickets);
    }

    @Transactional(readOnly = true)
    public PageResponse<MaintenanceTicketResponse> listMyMaintenanceTickets(UUID ownerId, Pageable pageable) {
        Specification<MaintenanceTicket> spec = MaintenanceTicketSpecifications.hasPropertyOwner(ownerId);
        Page<MaintenanceTicket> page = maintenanceTicketRepository.findAll(spec, pageable);
        return PageResponse.of(page, this::toMaintenanceTicketResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> listMyExpenses(UUID ownerId, LocalDate from, LocalDate to,
                                                          Pageable pageable) {
        Specification<Expense> spec = ExpenseSpecifications.combine(
                ExpenseSpecifications.hasPropertyOwner(ownerId),
                ExpenseSpecifications.chargeToOwnerOnly(),
                ExpenseSpecifications.dateFrom(from),
                ExpenseSpecifications.dateTo(to)
        );
        Page<Expense> page = expenseRepository.findAll(spec, pageable);
        return PageResponse.of(page, expenseMapper::toResponse);
    }

    private MaintenanceTicketResponse toMaintenanceTicketResponse(MaintenanceTicket ticket) {
        var photos = maintenanceTicketPhotoRepository.findByMaintenanceTicketIdOrderByCreatedAtAsc(ticket.getId());
        return maintenanceTicketMapper.toResponse(ticket, photos);
    }

    private OwnerPropertyResponse toOwnerPropertyResponse(Property property) {
        var photos = propertyPhotoRepository.findByPropertyIdOrderBySortOrderAsc(property.getId());
        var documents = propertyDocumentRepository.findByPropertyIdOrderByCreatedAtDesc(property.getId());
        BigDecimal revenue = reservationRepository
                .sumRevenueForProperty(property.getId(), ReservationStatus.NON_BLOCKING);
        return ownerMapper.toResponse(property, photos, revenue, documents);
    }
}
