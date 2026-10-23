package com.example.racetransport.config;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cấu hình riêng của ứng dụng (prefix "app"), đọc từ application.yml / biến môi trường. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String timeZone,
        Jwt jwt,
        Cors cors,
        Storage storage,
        Business business,
        Seed seed) {

    public record Jwt(String secret, long expirationMinutes, String issuer) { }

    public record Cors(List<String> allowedOrigins) { }

    public record Storage(String uploadDir, long maxFileSizeBytes, List<String> allowedTypes) { }

    /** Tham số nghiệp vụ có thể cấu hình. */
    public record Business(BigDecimal depositPercent, int quoteValidityDays, int invoiceDueDays) { }

    public record Seed(boolean demoData) { }
}
