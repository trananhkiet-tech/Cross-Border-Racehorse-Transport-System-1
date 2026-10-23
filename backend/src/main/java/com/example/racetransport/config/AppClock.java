package com.example.racetransport.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Clock của ứng dụng = đồng hồ hệ thống. Riêng bộ seed dữ liệu demo (profile dev) có thể
 * tạm "đặt giờ" để tạo dữ liệu lịch sử nhất quán thông qua chính các service nghiệp vụ,
 * sau đó gọi {@link #reset()} trước khi ứng dụng nhận request.
 */
public class AppClock extends Clock {

    private final ZoneId zone;
    private volatile Duration offset = Duration.ZERO;

    public AppClock(ZoneId zone) {
        this.zone = zone;
    }

    /** Chỉ dùng khi seed dữ liệu demo. */
    public void setNow(LocalDateTime target) {
        Instant real = Instant.now();
        this.offset = Duration.between(real, target.atZone(zone).toInstant());
    }

    public void reset() {
        this.offset = Duration.ZERO;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId z) {
        AppClock c = new AppClock(z);
        c.offset = this.offset;
        return c;
    }

    @Override
    public Instant instant() {
        return Instant.now().plus(offset);
    }
}
