package com.bhgroup.pms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * propertyName is a snapshot taken at generation time, not a live lookup -
 * property is a soft reference (nullable, ON DELETE SET NULL) so this line
 * keeps its meaning even if the property is later deleted.
 */
@Getter
@Setter
@Entity
@Table(name = "owner_statement_lines")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true, exclude = {"statement", "property"})
@EqualsAndHashCode(callSuper = true)
public class OwnerStatementLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id", nullable = false)
    private OwnerStatement statement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @Column(name = "property_name", nullable = false)
    private String propertyName;

    @Column(name = "gross_revenue", nullable = false)
    private BigDecimal grossRevenue;

    @Column(name = "commission_amount", nullable = false)
    private BigDecimal commissionAmount;

    @Column(name = "expenses_total", nullable = false)
    private BigDecimal expensesTotal;

    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount;
}
