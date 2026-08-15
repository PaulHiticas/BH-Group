package com.bhgroup.pms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
 * Opt-in dynamic pricing settings for a single property (one row per
 * property, enforced by the unique FK). When {@link #enabled} is false,
 * {@code PricingService} applies none of these factors and the quote is
 * identical to the plain seasonal/weekend/base calculation.
 */
@Getter
@Setter
@Entity
@Table(name = "dynamic_pricing_config")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true, exclude = "property")
@EqualsAndHashCode(callSuper = true)
public class DynamicPricingConfig extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false, unique = true)
    private Property property;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = false;

    /** Guardrails applied after every other factor - have the final say on the nightly rate. */
    @Column(name = "min_price", precision = 10, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 10, scale = 2)
    private BigDecimal maxPrice;

    @Column(name = "occupancy_window_days", nullable = false)
    @Builder.Default
    private int occupancyWindowDays = 14;

    @Column(name = "occupancy_multiplier_min", precision = 4, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal occupancyMultiplierMin = new BigDecimal("0.90");

    @Column(name = "occupancy_multiplier_max", precision = 4, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal occupancyMultiplierMax = new BigDecimal("1.30");

    @Column(name = "lead_time_days", nullable = false)
    @Builder.Default
    private int leadTimeDays = 7;

    @Column(name = "lead_time_multiplier", precision = 4, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal leadTimeMultiplier = BigDecimal.ONE;
}
