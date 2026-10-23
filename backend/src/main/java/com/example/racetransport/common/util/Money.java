package com.example.racetransport.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Tiện ích tiền tệ: luôn dùng BigDecimal, làm tròn 2 chữ số HALF_UP. */
public final class Money {

    public static final int SCALE = 2;

    private Money() { }

    public static BigDecimal of(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(SCALE) : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(String value) {
        return of(new BigDecimal(value));
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE);
    }

    public static boolean isPositive(BigDecimal v) {
        return v != null && v.signum() > 0;
    }

    /** Phần trăm của một số tiền, ví dụ percent(1000, 50) = 500.00 */
    public static BigDecimal percent(BigDecimal amount, BigDecimal percent) {
        return of(amount.multiply(percent).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
    }
}
