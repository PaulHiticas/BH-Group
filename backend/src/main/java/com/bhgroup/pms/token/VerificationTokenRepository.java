package com.bhgroup.pms.token;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByTokenAndType(String token, VerificationTokenType type);

    @Modifying
    @Query("update VerificationToken v set v.usedAt = CURRENT_TIMESTAMP where v.user.id = :userId "
            + "and v.type = :type and v.usedAt is null")
    int invalidateActiveTokens(@Param("userId") UUID userId, @Param("type") VerificationTokenType type);
}
