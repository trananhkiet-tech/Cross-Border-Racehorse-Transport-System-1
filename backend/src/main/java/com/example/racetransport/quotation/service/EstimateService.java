package com.example.racetransport.quotation.service;

import com.example.racetransport.common.service.CodeGenerator;
import com.example.racetransport.common.util.Money;
import com.example.racetransport.quotation.entity.CostCategory;
import com.example.racetransport.quotation.entity.PricingRate;
import com.example.racetransport.quotation.entity.PricingUnit;
import com.example.racetransport.quotation.entity.Quote;
import com.example.racetransport.quotation.entity.QuoteItem;
import com.example.racetransport.quotation.entity.QuoteStatus;
import com.example.racetransport.quotation.entity.QuoteType;
import com.example.racetransport.quotation.repository.PricingRateRepository;
import com.example.racetransport.quotation.repository.QuoteItemRepository;
import com.example.racetransport.quotation.repository.QuoteRepository;
import com.example.racetransport.transportationrequest.entity.RequestHorse;
import com.example.racetransport.transportationrequest.entity.TransportMode;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.RequestHorseRepository;
import com.example.racetransport.user.entity.AppUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sinh Estimated Cost từ bảng giá DEMO khi khách gửi yêu cầu.
 * KHÔNG tính theo "giá trị ngựa × đơn giá/km": giá trị khai báo chỉ dùng cho dòng phí bảo hiểm (%).
 */
@Service
public class EstimateService {

    /** Số ngày kiểm dịch/lưu chuồng giả định cho ước tính (Manager điều chỉnh ở báo giá cuối). */
    static final int DEFAULT_QUARANTINE_DAYS = 7;
    /** Quãng đường bộ trung chuyển tới/từ sân bay khi đi kết hợp. */
    static final BigDecimal ROAD_AIR_GROUND_KM = BigDecimal.valueOf(150);

    private final QuoteRepository quotes;
    private final QuoteItemRepository quoteItems;
    private final PricingRateRepository rates;
    private final RequestHorseRepository requestHorses;
    private final DistanceEstimator distance;
    private final CodeGenerator codes;

    public EstimateService(QuoteRepository quotes, QuoteItemRepository quoteItems, PricingRateRepository rates,
                           RequestHorseRepository requestHorses, DistanceEstimator distance, CodeGenerator codes) {
        this.quotes = quotes;
        this.quoteItems = quoteItems;
        this.rates = rates;
        this.requestHorses = requestHorses;
        this.distance = distance;
        this.codes = codes;
    }

    /** Tạo phiên bản ước tính mới; phiên bản ước tính cũ chuyển SUPERSEDED (giữ lịch sử). */
    @Transactional
    public Quote generateEstimate(TransportationRequest r, AppUser actor) {
        quotes.findByRequestIdAndStatusIn(r.getId(), EnumSet.of(QuoteStatus.ESTIMATED))
                .forEach(q -> q.setStatus(QuoteStatus.SUPERSEDED));
        List<RequestHorse> horses = requestHorses.findByRequestIdFetch(r.getId());
        List<QuoteItem> lines = buildLines(r, horses);

        Quote q = new Quote();
        q.setRequest(r);
        q.setQuoteNumber(codes.next(CodeGenerator.QUOTE));
        q.setVersionNo(nextVersion(r.getId()));
        q.setQuoteType(QuoteType.ESTIMATE);
        q.setStatus(QuoteStatus.ESTIMATED);
        q.setCurrency(r.getCurrency());
        q.setCreatedBy(actor);
        q.setNotes("Chi phí ước tính tự động từ bảng giá demo; báo giá cuối có thể khác sau khi xác nhận chi phí thực.");
        applyTotals(q, lines);
        q.setEstimatedTotal(q.getTotalAmount());
        quotes.save(q);
        int order = 0;
        for (QuoteItem it : lines) {
            it.setQuote(q);
            it.setSortOrder(order++);
            it.setEstimatedAmount(it.getAmount());
            quoteItems.save(it);
        }
        return q;
    }

    List<QuoteItem> buildLines(TransportationRequest r, List<RequestHorse> horses) {
        BigDecimal horseCount = BigDecimal.valueOf(horses.size());
        BigDecimal km = distance.estimateKm(r);
        int days = distance.estimateDays(r, km);
        boolean international = !r.getOriginCountry().equals(r.getDestinationCountry());
        TransportMode mode = r.getTransportMode();
        List<QuoteItem> lines = new ArrayList<>();
        for (PricingRate rate : rates.findApplicable(mode, r.getDestinationCountry())) {
            CostCategory c = rate.getCategory();
            if (c == CostCategory.CUSTOMS && !international) {
                continue;
            }
            if (c == CostCategory.AIR_TRANSPORT && mode == TransportMode.ROAD) {
                continue;
            }
            if (c == CostCategory.GROUND_TRANSPORT && mode == TransportMode.AIR) {
                continue;
            }
            if (c == CostCategory.QUARANTINE_STABLING && rate.getDestinationCountry() == null) {
                continue; // chỉ áp dụng khi nước đến có yêu cầu cách ly trong bảng giá demo
            }
            BigDecimal qty;
            BigDecimal unit = Money.of(rate.getUnitPrice());
            String desc = rate.getDescription();
            switch (rate.getUnit()) {
                case PER_KM -> {
                    qty = mode == TransportMode.ROAD_AIR ? ROAD_AIR_GROUND_KM : km;
                    desc = desc + " (" + qty.stripTrailingZeros().toPlainString() + " km ước tính)";
                }
                case PER_HORSE -> qty = horseCount;
                case PER_TRIP -> {
                    qty = c == CostCategory.ESCORT ? BigDecimal.valueOf(days) : BigDecimal.ONE;
                    if (c == CostCategory.ESCORT) {
                        desc = desc + " (" + days + " ngày)";
                    }
                }
                case PER_HORSE_DAY -> {
                    qty = horseCount.multiply(BigDecimal.valueOf(DEFAULT_QUARANTINE_DAYS));
                    desc = desc + " (" + horses.size() + " ngựa × " + DEFAULT_QUARANTINE_DAYS + " ngày)";
                }
                case PERCENT_OF_DECLARED_VALUE -> {
                    // Phí bảo hiểm = % giá trị khai báo; KHÔNG phải giá vận chuyển
                    qty = BigDecimal.ONE;
                    unit = Money.of(r.getDeclaredValueTotal().multiply(rate.getUnitPrice())
                            .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
                    desc = desc + " (" + rate.getUnitPrice().stripTrailingZeros().toPlainString()
                            + "% giá trị khai báo)";
                }
                default -> qty = BigDecimal.ONE;
            }
            if (qty.signum() <= 0) {
                continue;
            }
            lines.add(line(c, desc, qty, unit, "Bảng giá " + rate.getCode()));
        }
        // Phụ phí loại chuồng theo từng ngựa (Figma: tiêu chuẩn / rộng / hộp riêng)
        for (RequestHorse rh : horses) {
            BigDecimal surcharge = rh.getStallType().getSurcharge();
            if (surcharge != null && surcharge.signum() > 0) {
                lines.add(line(CostCategory.SURCHARGE, "Phụ phí chuồng " + rh.getStallType().getName() + " ("
                                + rh.getStallType().getWidthCm() + "cm) – " + rh.getHorse().getRegisteredName(),
                        BigDecimal.ONE, Money.of(surcharge), "Loại chuồng " + rh.getStallType().getCode()));
            }
        }
        return lines;
    }

    private static QuoteItem line(CostCategory c, String desc, BigDecimal qty, BigDecimal unit, String source) {
        QuoteItem it = new QuoteItem();
        it.setCategory(c);
        it.setDescription(desc.length() > 255 ? desc.substring(0, 254) : desc);
        it.setQuantity(qty.setScale(2, RoundingMode.HALF_UP));
        it.setUnitPrice(unit);
        it.setAmount(QuoteCalculator.lineAmount(c, it.getQuantity(), unit));
        it.setConfirmed(false);
        it.setSource(source);
        return it;
    }

    static void applyTotals(Quote q, List<QuoteItem> lines) {
        QuoteCalculator.Totals t = QuoteCalculator.totals(lines.stream()
                .map(i -> new QuoteCalculator.Line(i.getCategory(), i.getQuantity(), i.getUnitPrice())).toList());
        q.setSubtotal(t.subtotal());
        q.setDiscountTotal(t.discountTotal());
        q.setTotalAmount(t.total());
    }

    int nextVersion(Long requestId) {
        return quotes.findFirstByRequestIdOrderByVersionNoDesc(requestId).map(q -> q.getVersionNo() + 1).orElse(1);
    }

    /** Khi yêu cầu bị hủy/từ chối: đóng các báo giá còn mở. */
    @Transactional
    public void closeOpenQuotes(TransportationRequest r) {
        quotes.findByRequestIdAndStatusIn(r.getId(), EnumSet.of(QuoteStatus.DRAFT, QuoteStatus.SENT))
                .forEach(q -> q.setStatus(QuoteStatus.SUPERSEDED));
    }
}
