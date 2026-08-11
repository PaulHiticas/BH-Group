package com.bhgroup.pms.dto.report;

import java.util.List;

/**
 * {@code totals} has one entry per currency actually present in {@code rows}
 * - amounts in different currencies are never added together without a
 * conversion, so a portfolio with both RON and EUR activity gets two totals
 * entries instead of one misleading blended number.
 */
public record FinancialReportSummaryResponse(
        List<FinancialReportRowResponse> rows,
        List<FinancialReportCurrencyTotals> totals
) {
}
