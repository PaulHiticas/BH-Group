package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.MfaRecoveryCode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, UUID> {

    List<MfaRecoveryCode> findByUserIdAndUsedAtIsNull(UUID userId);

    Optional<MfaRecoveryCode> findByUserIdAndCodeHashAndUsedAtIsNull(UUID userId, String codeHash);

    void deleteByUserId(UUID userId);

    /**
     * Atomic consume: only flips usedAt when it is still null, so two
     * concurrent requests racing on the same recovery code can't both
     * succeed (the loser gets 0 affected rows instead of silently
     * overwriting the winner's usedAt).
     */
    @Modifying
    @Query("update MfaRecoveryCode c set c.usedAt = :now where c.id = :id and c.usedAt is null")
    int markUsedIfUnused(@Param("id") UUID id, @Param("now") Instant now);
}
