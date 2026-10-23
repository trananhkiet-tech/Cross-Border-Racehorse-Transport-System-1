package com.example.racetransport.quotation.service;

import com.example.racetransport.common.exception.BadRequestException;
import com.example.racetransport.common.util.Money;
import com.example.racetransport.quotation.entity.CostCategory;
import java.math.BigDecimal;
import java.util.List;

/**
 * Tính tiền báo giá ở BACKEND (không tin tổng do frontend gửi):
 * amount = quantity × unitPrice (làm tròn 2 số); dòng DISCOUNT luôn âm.
 * subtotal = tổng các dòng dương; discountTotal = |tổng dòng giảm giá|; total = subtotal − discountTotal (≥ 0).
 */
public final class QuoteCalculator {

    private QuoteCalculator() { }

    public record Line(CostCategory category, BigDecimal quantity, BigDecimal unitPrice) { }

    public record Totals(BigDecimal subtotal, BigDecimal discountTotal, BigDecimal total) { }

    public static BigDecimal lineAmount(CostCategory category, BigDecimal quantity, BigDecimal unitPrice) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BadRequestException("Số lượng phải lớn hơn 0");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new BadRequestException("Đơn giá không được âm (giảm giá nhập số dương ở hạng mục Giảm giá)");
        }
        BigDecimal amount = Money.of(quantity.multiply(unitPrice));
        return category == CostCategory.DISCOUNT ? amount.negate() : amount;
    }

    public static Totals totals(List<Line> lines) {
        BigDecimal subtotal = Money.zero();
        BigDecimal discount = Money.zero();
        for (Line l : lines) {
            BigDecimal amount = lineAmount(l.category(), l.quantity(), l.unitPrice());
            if (amount.signum() < 0) {
                discount = discount.add(amount.negate());
            } else {
                subtotal = subtotal.add(amount);
            }
        }
        BigDecimal total = subtotal.subtract(discount);
        if (total.signum() < 0) {
            throw new BadRequestException("Tổng giảm giá vượt quá tổng chi phí");
        }
        return new Totals(subtotal, discount, total);
    }
}
