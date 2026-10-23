package com.example.racetransport.config;

import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bean Clock duy nhất của ứng dụng (kiểu AppClock, cũng là java.time.Clock):
 * service lấy thời gian hiện tại qua Clock để dễ thay thế khi test / seed demo.
 */
@Configuration
public class TimeConfig {

    @Bean
    public AppClock clock() {
        return new AppClock(ZoneId.systemDefault());
    }
}
