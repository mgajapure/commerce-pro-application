package com.commerce_pro_backend.user_identity.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.commerce_pro_backend.user_identity.entity.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revoke all tokens in a family — used when token reuse (theft) is detected.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now, " +
           "rt.revocationReason = :reason WHERE rt.family = :family")
    int revokeAllByFamily(@Param("family") String family,
                          @Param("now") LocalDateTime now,
                          @Param("reason") String reason);

    /**
     * Revoke all active tokens for a user — used on logout.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now, " +
           "rt.revocationReason = :reason WHERE rt.username = :username AND rt.revoked = false")
    int revokeAllByUsername(@Param("username") String username,
                            @Param("now") LocalDateTime now,
                            @Param("reason") String reason);

    /**
     * Clean up expired tokens — can be called on a schedule.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") LocalDateTime before);

    boolean existsByFamilyAndRevokedFalse(String family);
}
