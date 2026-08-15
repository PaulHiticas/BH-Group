package com.bhgroup.pms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A date-ranged price multiplier scoped to either a whole {@link #city}
 * (applies to every property located there) or a single {@link #property}
 * (an override for just that one) - never both null, and never applied
 * globally to every property regardless of location.
 */
@Getter
@Setter
@Entity
@Table(name = "local_events")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true, exclude = "property")
@EqualsAndHashCode(callSuper = true)
public class LocalEvent extends BaseEntity {

    /**
     * Matched against a property's city case-insensitively and trimmed
     * (see PricingService) - null when this event is a property-specific
     * override instead.
     */
    @Column(name = "city", length = 120)
    private String city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "price_multiplier", precision = 4, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal priceMultiplier = BigDecimal.ONE;
}
