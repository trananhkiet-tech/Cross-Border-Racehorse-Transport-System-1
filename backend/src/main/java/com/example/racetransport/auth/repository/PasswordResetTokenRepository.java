package com.example.racetransport.auth.repository;

import com.example.racetransport.auth.entity.PasswordResetToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findByUserIdAndUsedAtIsNull(Long userId);

    Optional<PasswordResetToken> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
