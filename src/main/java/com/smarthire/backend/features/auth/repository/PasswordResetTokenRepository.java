package com.smarthire.backend.features.auth.repository;

import com.smarthire.backend.features.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenAndIsUsedFalse(String token);

    @Modifying
    @Query("UPDATE PasswordResetToken prt SET prt.isUsed = true WHERE prt.user.id = :userId AND prt.isUsed = false")
    void invalidateAllByUserId(@Param("userId") Long userId);
}
