package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.csv.CsvWriter;
import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.dto.report.FinancialReportSummaryResponse;
import com.bhgroup.pms.service.FinancialReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports/financial")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','ACCOUNTANT')")
@Tag(name = "Financial Reports", description = "Revenue, commission and expense reporting per property")
public class FinancialReportController {

    private final FinancialReportService financialReportService;

    @GetMapping("/summary")
    @Operation(summary = "Get gross revenue, commission, expenses and net profit per property")
    public ResponseEntity<ApiResponse<FinancialReportSummaryResponse>> summary(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(financialReportService.summary(propertyId, from, to)));
    }

    @GetMapping("/export")
    @Operation(summary = "Export the financial summary as CSV")
    public void export(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletResponse response) throws IOException {
        CsvWriter.write(response, "raport-financiar.csv",
                List.of("Proprietate", "Proprietar", "Venit brut", "Comision", "Cheltuieli", "Profit net", "Monedă"),
                financialReportService.exportRows(propertyId, from, to));
    }
}
