package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.csv.CsvWriter;
import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.ExpenseCategory;
import com.bhgroup.pms.dto.expense.ExpenseCreateRequest;
import com.bhgroup.pms.dto.expense.ExpenseResponse;
import com.bhgroup.pms.dto.expense.ExpenseUpdateRequest;
import com.bhgroup.pms.security.SecurityUtils;
import com.bhgroup.pms.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','ACCOUNTANT')")
@Tag(name = "Expenses", description = "Property expense tracking")
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    @Operation(summary = "List expenses with filters")
    public ResponseEntity<ApiResponse<PageResponse<ExpenseResponse>>> list(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) ExpenseCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.list(propertyId, category, from, to, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an expense by id")
    public ResponseEntity<ApiResponse<ExpenseResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.get(id)));
    }

    @GetMapping("/export")
    @Operation(summary = "Export expenses matching the given filters as CSV")
    public void export(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) ExpenseCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletResponse response) throws IOException {
        CsvWriter.write(response, "cheltuieli.csv",
                List.of("Data", "Proprietate", "Categorie", "Sumă", "Monedă", "Furnizor", "Facturat proprietarului", "Note"),
                expenseService.exportRows(propertyId, category, from, to));
    }

    @PostMapping
    @Operation(summary = "Record an expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> create(@Valid @RequestBody ExpenseCreateRequest request) {
        UUID userId = SecurityUtils.getCurrentPrincipal().map(p -> p.getId()).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(expenseService.create(request, userId), "Expense recorded"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ExpenseUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.update(id, request), "Expense updated"));
    }

    @PostMapping(value = "/{id}/receipt", consumes = "multipart/form-data")
    @Operation(summary = "Upload a receipt for an expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> uploadReceipt(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(expenseService.uploadReceipt(id, file)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an expense")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        expenseService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Expense deleted"));
    }
}
