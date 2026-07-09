package com.bhgroup.pms.property;

import com.bhgroup.pms.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

@Getter
@Setter
@Entity
@Table(name = "properties")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Property extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "property_type", nullable = false)
    @Builder.Default
    private PropertyType propertyType = PropertyType.APARTMENT;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    @Builder.Default
    private PropertyStatus status = PropertyStatus.DRAFT;

    @Embedded
    private Address address;

    @Column(nullable = false)
    @Builder.Default
    private int bedrooms = 0;

    @Column(nullable = false)
    @Builder.Default
    private int bathrooms = 0;

    @Column(name = "max_guests", nullable = false)
    @Builder.Default
    private int maxGuests = 1;

    @Column(name = "size_sqm")
    private BigDecimal sizeSqm;

    @Column(name = "base_price_per_night", precision = 10, scale = 2)
    private BigDecimal basePricePerNight;

    @Column(name = "check_in_time", nullable = false)
    @Builder.Default
    private LocalTime checkInTime = LocalTime.of(14, 0);

    @Column(name = "check_out_time", nullable = false)
    @Builder.Default
    private LocalTime checkOutTime = LocalTime.of(11, 0);

    @Column(name = "smart_lock_enabled", nullable = false)
    @Builder.Default
    private boolean smartLockEnabled = false;

    @Column(name = "smart_lock_provider")
    private String smartLockProvider;

    @Column(name = "smart_lock_device_id")
    private String smartLockDeviceId;

    @ElementCollection(targetClass = Facility.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "property_facilities", joinColumns = @JoinColumn(name = "property_id"))
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "facility")
    @Builder.Default
    private Set<Facility> facilities = new HashSet<>();

    @CreatedBy
    @Column(name = "created_by")
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;
}
