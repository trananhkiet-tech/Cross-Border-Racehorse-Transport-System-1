package com.example.racetransport.payment.service;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.common.service.CodeGenerator;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.common.util.Money;
import com.example.racetransport.config.AppProperties;
import com.example.racetransport.payment.dto.PaymentDtos.InvoiceResponse;
import com.example.racetransport.payment.dto.PaymentDtos.PaymentResponse;
import com.example.racetransport.payment.entity.Invoice;
import com.example.racetransport.payment.entity.InvoiceStatus;
import com.example.racetransport.payment.repository.InvoiceRepository;
import com.example.racetransport.payment.repository.PaymentRepository;
import com.example.racetransport.quotation.entity.Quote;
import com.example.racetransport.quotation.entity.QuoteStatus;
import com.example.racetransport.quotation.entity.QuoteType;
import com.example.racetransport.quotation.repository.QuoteRepository;
import com.example.racetransport.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Hóa đơn phát hành từ báo giá cuối đã được chấp nhận. Số tiền lấy từ báo giá (backend). */
@Service
public class InvoiceService {

    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final QuoteRepository quotes;
    private final CodeGenerator codes;
    private final AppProperties props;
    private final Clock clock;

    public InvoiceService(InvoiceRepository invoices, PaymentRepository payments, QuoteRepository quotes,
                          CodeGenerator codes, AppProperties props, Clock clock) {
        this.invoices = invoices;
        this.payments = payments;
        this.quotes = quotes;
        this.codes = codes;
        this.props = props;
        this.clock = clock;
    }

    /** Idempotent: mỗi báo giá có tối đa một hóa đơn. */
    @Transactional
    public Invoice createForQuote(Quote q) {
        Guard.state(q.getQuoteType() == QuoteType.FINAL && q.getStatus() == QuoteStatus.ACCEPTED,
                "Chỉ phát hành hóa đơn cho báo giá cuối đã được chấp nhận");
        return invoices.findByQuoteId(q.getId()).orElseGet(() -> {
            Invoice inv = new Invoice();
            inv.setInvoiceNumber(codes.next(CodeGenerator.INVOICE));
            inv.setQuote(q);
            inv.setRequest(q.getRequest());
            inv.setCustomer(q.getRequest().getCustomer());
            LocalDate today = LocalDate.now(clock);
            inv.setIssueDate(today);
            inv.setDueDate(today.plusDays(props.business().invoiceDueDays()));
            inv.setCurrency(q.getCurrency());
            inv.setTotalAmount(Money.of(q.getTotalAmount()));
            inv.setPaidAmount(Money.zero());
            inv.setDepositPercent(props.business().depositPercent());
            inv.setStatus(InvoiceStatus.ISSUED);
            return invoices.save(inv);
        });
    }

    /** POST /api/invoices: Manager phát hành (lại) hóa đơn cho báo giá đã chấp nhận nếu chưa có. */
    @Transactional
    public InvoiceResponse create(UserPrincipal actor, Long quoteId) {
        if (!actor.isManager()) {
            throw new ForbiddenException("Chỉ Logistics Manager phát hành hóa đơn");
        }
        Quote q = quotes.findById(quoteId).orElseThrow(() -> new NotFoundException("Không tìm thấy báo giá"));
        return toResponse(createForQuote(q));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(UserPrincipal actor, Long id) {
        return toResponse(loadVisible(actor, id));
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> list(UserPrincipal actor, Collection<InvoiceStatus> statuses, int page,
                                              int size) {
        if (!actor.isCustomer() && !actor.isManager()) {
            throw new ForbiddenException("Không có quyền xem hóa đơn");
        }
        var result = invoices.search(actor.isCustomer() ? actor.getId() : null,
                statuses == null || statuses.isEmpty() ? EnumSet.allOf(InvoiceStatus.class) : statuses,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "issueDate")));
        return PageResponse.of(result, this::toResponse);
    }

    public Invoice loadVisible(UserPrincipal actor, Long id) {
        Invoice inv = invoices.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn"));
        if (actor.isCustomer() && !inv.getCustomer().getId().equals(actor.getId())) {
            throw new ForbiddenException("Bạn không có quyền xem hóa đơn này");
        }
        if (!actor.isCustomer() && !actor.isManager()) {
            throw new ForbiddenException("Không có quyền xem hóa đơn");
        }
        return inv;
    }

    /** Số tiền đặt cọc yêu cầu (theo % cấu hình). */
    public static BigDecimal depositAmount(Invoice inv) {
        return Money.percent(inv.getTotalAmount(), inv.getDepositPercent());
    }

    /** Lần thanh toán đầu phải tối thiểu bằng tiền cọc; sau đó tối thiểu 0.01. */
    public static BigDecimal minimumNextPayment(Invoice inv) {
        BigDecimal outstanding = inv.getTotalAmount().subtract(inv.getPaidAmount());
        if (outstanding.signum() <= 0) {
            return Money.zero();
        }
        if (inv.getPaidAmount().signum() == 0) {
            return depositAmount(inv).min(outstanding);
        }
        return Money.of("0.01");
    }

    public InvoiceResponse toResponse(Invoice inv) {
        BigDecimal outstanding = Money.of(inv.getTotalAmount().subtract(inv.getPaidAmount()));
        boolean overdue = outstanding.signum() > 0 && inv.getStatus() != InvoiceStatus.CANCELLED
                && inv.getDueDate().isBefore(LocalDate.now(clock));
        List<PaymentResponse> list = payments.findByInvoiceIdOrderByCreatedAtDesc(inv.getId()).stream()
                .map(p -> PaymentService.toResponse(p, inv)).toList();
        return new InvoiceResponse(inv.getId(), inv.getInvoiceNumber(), inv.getQuote().getId(),
                inv.getQuote().getQuoteNumber(), inv.getRequest().getId(), inv.getRequest().getRequestNumber(),
                inv.getCustomer().getId(), inv.getCustomer().getFullName(), inv.getIssueDate(), inv.getDueDate(),
                inv.getCurrency(), inv.getTotalAmount(), inv.getPaidAmount(), outstanding, inv.getDepositPercent(),
                depositAmount(inv), minimumNextPayment(inv),
                overdue && inv.getStatus() != InvoiceStatus.PAID ? InvoiceStatus.OVERDUE : inv.getStatus(),
                overdue, list);
    }
}
