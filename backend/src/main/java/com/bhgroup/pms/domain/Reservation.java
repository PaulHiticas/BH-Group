package com.bhgroup.pms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "reservations")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true, exclude = "property")
@EqualsAndHashCode(callSuper = true)
public class Reservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "guest_first_name", nullable = false)
    private String guestFirstName;

    @Column(name = "guest_last_name", nullable = false)
    private String guestLastName;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(name = "guest_phone")
    private String guestPhone;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "number_of_guests", nullable = false)
    @Builder.Default
    private int numberOfGuests = 1;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    @Builder.Default
    private ReservationSource source = ReservationSource.DIRECT;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "RON";

    @Column(length = 2000)
    private String notes;

    @Column(name = "management_token", unique = true)
    private String managementToken;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    /** Stable event UID from an imported Airbnb/Booking.com .ics feed; null for reservations created in the PMS. */
    @Column(name = "external_uid", unique = true)
    private String externalUid;

    @Column(name = "access_code", length = 50)
    private String accessCode;

    @Column(name = "access_code_sent_at")
    private Instant accessCodeSentAt;

    @CreatedBy
    @Column(name = "created_by")
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    public String getGuestFullName() {
        return guestFirstName + " " + guestLastName;
    }
}
