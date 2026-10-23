package com.example.racetransport.security;

import com.example.racetransport.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Ký và kiểm tra JWT (HS256). Khóa lấy từ biến môi trường JWT_SECRET (≥ 32 ký tự).
 * Profile prod bắt buộc có JWT_SECRET; profile dev nếu thiếu sẽ dùng khóa ngẫu nhiên tạm thời.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int MIN_SECRET_LENGTH = 32;

    private final SecretKey key;
    private final long expirationMinutes;
    private final String issuer;

    public JwtService(AppProperties props, Environment env) {
        String secret = props.jwt().secret();
        boolean prod = Arrays.asList(env.getActiveProfiles()).contains("prod");
        if (secret == null || secret.isBlank()) {
            if (prod) {
                throw new IllegalStateException("JWT_SECRET bat buoc o profile prod");
            }
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            this.key = Keys.hmacShaKeyFor(random);
            log.warn("JWT_SECRET chua duoc dat: dung khoa tam thoi (token mat hieu luc khi khoi dong lai). Chi dung cho dev.");
        } else {
            if (secret.length() < MIN_SECRET_LENGTH) {
                throw new IllegalStateException("JWT_SECRET phai dai it nhat 32 ky tu");
            }
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.expirationMinutes = props.jwt().expirationMinutes();
        this.issuer = props.jwt().issuer();
    }

    public String generateToken(UserPrincipal user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuer(issuer)
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(key)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }

    /** Trả về user id nếu token hợp lệ và chưa hết hạn. */
    public Optional<Long> parseUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(Long.valueOf(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT khong hop le: {}", ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
