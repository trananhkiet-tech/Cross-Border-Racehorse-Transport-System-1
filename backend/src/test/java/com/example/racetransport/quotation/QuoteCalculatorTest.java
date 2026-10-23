package com.example.racetransport.quotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.racetransport.common.exception.BadRequestException;
import com.example.racetransport.quotation.entity.CostCategory;
import com.example.racetransport.quotation.service.QuoteCalculator;
import com.example.racetransport.quotation.service.QuoteCalculator.Line;
import com.example.racetransport.quotation.service.QuoteCalculator.Totals;
import com.example.racetransport.support.TestData;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tổng báo giá được tính ở backend, gồm phụ phí và giảm giá. */
class QuoteCalculatorTest {

    @Test
    void totalsIncludeSurchargeAndSubtractDiscount() {
        Totals t = QuoteCalculator.totals(List.of(
                new Line(CostCategory.GROUND_TRANSPORT, new BigDecimal("150"), new BigDecimal("2.80")),
                new Line(CostCategory.AIR_TRANSPORT, new BigDecimal("2"), new BigDecimal("6500")),
                new Line(CostCategory.SURCHARGE, BigDecimal.ONE, new BigDecimal("650")),
                new Line(CostCategory.DISCOUNT, BigDecimal.ONE, new BigDecimal("520"))));
        assertTrue(TestData.same("14070.00", t.subtotal()), "subtotal = 420 + 13000 + 650");
        assertTrue(TestData.same("520.00", t.discountTotal()));
        assertTrue(TestData.same("13550.00", t.total()));
    }

    @Test
    void discountLineIsAlwaysNegative() {
        BigDecimal amount = QuoteCalculator.lineAmount(CostCategory.DISCOUNT, new BigDecimal("1"), new BigDecimal("300"));
        assertEquals(-1, amount.signum());
    }

    @Test
    void lineAmountIsRoundedToCents() {
        BigDecimal amount = QuoteCalculator.lineAmount(CostCategory.HANDLING, new BigDecimal("3"), new BigDecimal("33.335"));
        assertEquals(new BigDecimal("100.01"), amount);
    }

    @Test
    void negativeUnitPriceIsRejected() {
        assertThrows(BadRequestException.class,
                () -> QuoteCalculator.lineAmount(CostCategory.SURCHARGE, BigDecimal.ONE, new BigDecimal("-5")));
    }

    @Test
    void zeroQuantityIsRejected() {
        assertThrows(BadRequestException.class,
                () -> QuoteCalculator.lineAmount(CostCategory.HANDLING, BigDecimal.ZERO, new BigDecimal("5")));
    }

    @Test
    void discountGreaterThanCostsIsRejected() {
        assertThrows(BadRequestException.class, () -> QuoteCalculator.totals(List.of(
                new Line(CostCategory.DOCUMENTS, BigDecimal.ONE, new BigDecimal("100")),
                new Line(CostCategory.DISCOUNT, BigDecimal.ONE, new BigDecimal("150")))));
    }
}
