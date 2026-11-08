package pe.techsupport.auth.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import pe.techsupport.auth.domain.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Transactional
    @Modifying
    @Query("update RefreshToken r set r.revoked = true, r.revokedAt = :now where r.familyId = :familyId and r.revoked = false")
    int revokeFamily(@Param("familyId") String familyId, @Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("update RefreshToken r set r.revoked = true, r.revokedAt = :now where r.userId = :userId and r.revoked = false")
    int revokeAllForUser(@Param("userId") String userId, @Param("now") Instant now);
}
