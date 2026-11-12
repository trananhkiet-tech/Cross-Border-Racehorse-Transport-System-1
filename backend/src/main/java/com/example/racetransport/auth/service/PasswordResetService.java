package com.example.racetransport.auth.service;

import com.example.racetransport.auth.dto.AuthDtos.ForgotPasswordResponse;
import com.example.racetransport.auth.entity.PasswordResetToken;
import com.example.racetransport.auth.repository.PasswordResetTokenRepository;
import com.example.racetransport.common.exception.BadRequestException;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.repository.AppUserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quên / đặt lại mật khẩu.
 * - Token ngẫu nhiên 256 bit, chỉ lưu băm SHA-256, dùng một lần, hết hạn sau {@code expirationMinutes}.
 * - Phản hồi luôn giống nhau dù email có tồn tại hay không (chống dò tài khoản).
 * - Tối thiểu {@link #MIN_INTERVAL_SECONDS} giây giữa hai lần yêu cầu cho cùng một tài khoản.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    static final String GENERIC_MESSAGE =
            "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư.";
    static final long MIN_INTERVAL_SECONDS = 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailer mailer;
    private final Clock clock;
    private final long expirationMinutes;
    private final String frontendUrl;
    private final boolean exposeToken;

    public PasswordResetService(AppUserRepository users, PasswordResetTokenRepository tokens, PasswordEncoder passwordEncoder,
                                PasswordResetMailer mailer, Clock clock,
                                @Value("${app.password-reset.expiration-minutes:30}") long expirationMinutes,
                                @Value("${app.password-reset.frontend-url:http://localhost:5173}") String frontendUrl,
                                @Value("${app.password-reset.expose-token:false}") boolean exposeToken) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.mailer = mailer;
        this.clock = clock;
        this.expirationMinutes = expirationMinutes;
        this.frontendUrl = frontendUrl;
        this.exposeToken = exposeToken;
    }

    @Transactional
    public ForgotPasswordResponse requestReset(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
        AppUser user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return new ForgotPasswordResponse(GENERIC_MESSAGE, null);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        boolean tooSoon = tokens.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .map(t -> t.getCreatedAt().plusSeconds(MIN_INTERVAL_SECONDS).isAfter(now)).orElse(false);
        if (tooSoon) {
            return new ForgotPasswordResponse(GENERIC_MESSAGE, null);
        }
        // Vô hiệu các token cũ chưa dùng: chỉ link mới nhất còn hiệu lực.
        for (PasswordResetToken old : tokens.findByUserIdAndUsedAtIsNull(user.getId())) {
            old.setUsedAt(now);
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        PasswordResetToken t = new PasswordResetToken();
        t.setUser(user);
        t.setTokenHash(hash(token));
        t.setCreatedAt(now);
        t.setExpiresAt(now.plusMinutes(expirationMinutes));
        tokens.save(t);

        String link = frontendUrl.replaceAll("/+$", "") + "/reset-password?token=" + token;
        boolean sent = mailer.send(user.getEmail(), user.getFullName(), link, expirationMinutes);
        log.info("Password reset requested for user id={} (mail sent: {})", user.getId(), sent);
        if (!sent && exposeToken) {
            // Chỉ cho môi trường dev khi chưa cấu hình SMTP; không bao giờ bật ở production.
            return new ForgotPasswordResponse(GENERIC_MESSAGE, token);
        }
        return new ForgotPasswordResponse(GENERIC_MESSAGE, null);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken t = tokens.findByTokenHash(hash(token == null ? "" : token.trim()))
                .orElseThrow(() -> new BadRequestException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"));
        LocalDateTime now = LocalDateTime.now(clock);
        if (t.getUsedAt() != null || !t.getExpiresAt().isAfter(now)) {
            throw new BadRequestException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
        }
        AppUser user = t.getUser();
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BadRequestException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        t.setUsedAt(now);
        for (PasswordResetToken other : tokens.findByUserIdAndUsedAtIsNull(user.getId())) {
            other.setUsedAt(now);
        }
        log.info("Password reset completed for user id={}", user.getId());
    }

    static String hash(String token) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
