package com.example.racetransport;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * IRTMS – International Racehorse Transportation Management System.
 * Điểm khởi động backend Spring Boot.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class RacehorseTransportApplication {

    public static void main(String[] args) {
        // Múi giờ nghiệp vụ: mọi LocalDateTime là giờ địa phương của doanh nghiệp (mặc định UTC+7)
        String zone = System.getenv().getOrDefault("APP_TIME_ZONE", "Asia/Ho_Chi_Minh");
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        SpringApplication.run(RacehorseTransportApplication.class, args);
    }
}
