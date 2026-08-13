package com.bhgroup.pms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Compliance register of resolved GDPR data-subject requests - deliberately
 * separate from the general audit log, and deliberately never stores the
 * full email (see {@link #maskedEmail}): only enough is kept to prove a
 * request for a given address was handled, when, by whom, and how identity
 * was checked, without permanently retaining the PII the request was about.
 * How long this register itself should be kept is a legal question, not
 * decided here - rows are written but nothing purges them yet.
 */
@Getter
@Setter
@Entity
@Table(name = "gdpr_requests")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true, exclude = "actor")
@EqualsAndHashCode(callSuper = true)
public class GdprRequest extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "request_type", nullable = false)
    private GdprRequestType requestType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    @Builder.Default
    private GdprRequestStatus status = GdprRequestStatus.COMPLETED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "masked_email", nullable = false)
    private String maskedEmail;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "verification_method", nullable = false)
    private GdprVerificationMethod verificationMethod;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    @Column(name = "records_affected", nullable = false)
    private int recordsAffected;
}
