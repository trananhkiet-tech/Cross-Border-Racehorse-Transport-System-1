package com.example.racetransport.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Gửi email đặt lại mật khẩu. Chỉ gửi thật khi đã cấu hình SMTP (spring.mail.host);
 * nếu chưa, trả về false và ứng dụng vẫn chạy bình thường.
 */
@Component
public class PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);

    private final ObjectProvider<JavaMailSender> sender;
    private final String from;

    public PasswordResetMailer(ObjectProvider<JavaMailSender> sender,
                               @Value("${app.password-reset.mail-from:no-reply@irtms.vn}") String from) {
        this.sender = sender;
        this.from = from;
    }

    /** @return true nếu đã gửi; false nếu chưa cấu hình SMTP hoặc gửi lỗi. */
    public boolean send(String to, String fullName, String link, long expirationMinutes) {
        JavaMailSender s = sender.getIfAvailable();
        if (s == null) {
            log.warn("Chưa cấu hình SMTP (spring.mail.host) – không gửi được email đặt lại mật khẩu.");
            return false;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(to);
            m.setSubject("IRTMS – Đặt lại mật khẩu");
            m.setText("Xin chào " + fullName + ",\n\nBạn vừa yêu cầu đặt lại mật khẩu IRTMS. Mở liên kết sau (hiệu lực "
                    + expirationMinutes + " phút, dùng một lần):\n" + link
                    + "\n\nNếu không phải bạn yêu cầu, hãy bỏ qua email này.");
            s.send(m);
            return true;
        } catch (Exception e) {
            log.error("Gửi email đặt lại mật khẩu thất bại: {}", e.getMessage());
            return false;
        }
    }
}
