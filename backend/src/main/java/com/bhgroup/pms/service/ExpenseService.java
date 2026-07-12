package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.Expense;
import com.bhgroup.pms.domain.ExpenseCategory;
import com.bhgroup.pms.domain.MaintenanceTicket;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.dto.expense.ExpenseCreateRequest;
import com.bhgroup.pms.dto.expense.ExpenseResponse;
import com.bhgroup.pms.dto.expense.ExpenseUpdateRequest;
import com.bhgroup.pms.repository.ExpenseRepository;
import com.bhgroup.pms.repository.ExpenseSpecifications;
import com.bhgroup.pms.repository.MaintenanceTicketRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.service.mapper.ExpenseMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final PropertyRepository propertyRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ExpenseMapper expenseMapper;

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(UUID propertyId, ExpenseCategory category,
                                               LocalDate from, LocalDate to, Pageable pageable) {
        Specification<Expense> spec = ExpenseSpecifications.combine(
                ExpenseSpecifications.hasProperty(propertyId),
                ExpenseSpecifications.hasCategory(category),
                ExpenseSpecifications.dateFrom(from),
                ExpenseSpecifications.dateTo(to)
        );
        Page<Expense> page = expenseRepository.findAll(spec, pageable);
        return PageResponse.of(page, expenseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> listForOwner(UUID ownerId, UUID propertyId,
                                                        LocalDate from, LocalDate to, Pageable pageable) {
        Specification<Expense> spec = ExpenseSpecifications.combine(
                ExpenseSpecifications.hasPropertyOwner(ownerId),
                ExpenseSpecifications.chargeToOwnerOnly(),
                ExpenseSpecifications.hasProperty(propertyId),
                ExpenseSpecifications.dateFrom(from),
                ExpenseSpecifications.dateTo(to)
        );
        Page<Expense> page = expenseRepository.findAll(spec, pageable);
        return PageResponse.of(page, expenseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(UUID id) {
        return expenseMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public ExpenseResponse create(ExpenseCreateRequest request, UUID createdByUserId) {
        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        Expense expense = Expense.builder()
                .property(property)
                .category(request.category() != null ? request.category() : ExpenseCategory.OTHER)
                .amount(request.amount())
                .currency(request.currency() != null && !request.currency().isBlank() ? request.currency() : "RON")
                .vendor(request.vendor())
                .expenseDate(request.expenseDate())
                .notes(request.notes())
                .chargeToOwner(request.chargeToOwner())
                .build();

        if (request.maintenanceTicketId() != null) {
            MaintenanceTicket ticket = maintenanceTicketRepository.findById(request.maintenanceTicketId())
                    .orElseThrow(() -> new ResourceNotFoundException("Maintenance ticket not found"));
            expense.setMaintenanceTicket(ticket);
        }
        if (createdByUserId != null) {
            userRepository.findById(createdByUserId).ifPresent(expense::setCreatedBy);
        }

        return expenseMapper.toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse update(UUID id, ExpenseUpdateRequest request) {
        Expense expense = findOrThrow(id);
        if (request.category() != null) expense.setCategory(request.category());
        expense.setAmount(request.amount());
        if (request.currency() != null && !request.currency().isBlank()) expense.setCurrency(request.currency());
        expense.setVendor(request.vendor());
        expense.setExpenseDate(request.expenseDate());
        expense.setNotes(request.notes());
        expense.setChargeToOwner(request.chargeToOwner());
        return expenseMapper.toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse uploadReceipt(UUID id, MultipartFile file) {
        Expense expense = findOrThrow(id);
        FileStorageService.StoredFile stored = fileStorageService.storeDocument(file, "expenses/" + expense.getId());
        expense.setReceiptFileKey(stored.fileKey());
        expense.setReceiptUrl(stored.url());
        return expenseMapper.toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(UUID id) {
        Expense expense = findOrThrow(id);
        if (expense.getReceiptFileKey() != null) {
            fileStorageService.delete(expense.getReceiptFileKey());
        }
        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public List<List<String>> exportRows(UUID propertyId, ExpenseCategory category, LocalDate from, LocalDate to) {
        Specification<Expense> spec = ExpenseSpecifications.combine(
                ExpenseSpecifications.hasProperty(propertyId),
                ExpenseSpecifications.hasCategory(category),
                ExpenseSpecifications.dateFrom(from),
                ExpenseSpecifications.dateTo(to)
        );
        return expenseRepository.findAll(spec, Sort.by("expenseDate").descending())
                .stream()
                .map(e -> List.of(
                        e.getExpenseDate().toString(),
                        e.getProperty().getName(),
                        e.getCategory().name(),
                        e.getAmount().toString(),
                        e.getCurrency(),
                        e.getVendor() != null ? e.getVendor() : "",
                        e.isChargeToOwner() ? "Da" : "Nu",
                        e.getNotes() != null ? e.getNotes() : ""
                ))
                .toList();
    }

    private Expense findOrThrow(UUID id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
    }
}
