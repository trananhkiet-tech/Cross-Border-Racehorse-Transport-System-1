package com.example.racetransport.payment.service;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.common.service.CodeGenerator;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.common.util.Money;
import com.example.racetransport.notification.entity.NotificationType;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.payment.dto.PaymentDtos.PaymentResponse;
import com.example.racetransport.payment.dto.PaymentDtos.SimulatePaymentRequest;
import com.example.racetransport.payment.entity.Invoice;
import com.example.racetransport.payment.entity.InvoiceStatus;
import com.example.racetransport.payment.entity.Payment;
import com.example.racetransport.payment.entity.PaymentMethod;
import com.example.racetransport.payment.entity.PaymentStatus;
import com.example.racetransport.payment.repository.PaymentRepository;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.service.RequestWorkflow;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thanh toán MÔ PHỎNG (không kết nối cổng thanh toán, không xử lý thẻ thật).
 * Backend quyết định số tiền hợp lệ, kết quả giao dịch và trạng thái hóa đơn;
 * client không thể tự đánh dấu "đã thanh toán".
 */
@Service
public class PaymentService {

    /** 4 số cuối mô phỏng giao dịch bị ngân hàng từ chối (phục vụ demo luồng lỗi). */
    public static final String DECLINED_CARD_LAST4 = "0002";

    private final PaymentRepository payments;
    private final InvoiceService invoiceService;
    private final RequestWorkflow workflow;
    private final NotificationService notifications;
    private final AppUserRepository users;
    private final CodeGenerator codes;
    private final Clock clock;

    public PaymentService(PaymentRepository payments, InvoiceService invoiceService, RequestWorkflow workflow,
                          NotificationService notifications, AppUserRepository users, CodeGenerator codes,
                          Clock clock) {
        this.payments = payments;
        this.invoiceService = invoiceService;
        this.workflow = workflow;
        this.notifications = notifications;
        this.users = users;
        this.codes = codes;
        this.clock = clock;
    }

    @Transactional
    public PaymentResponse simulate(UserPrincipal actor, SimulatePaymentRequest req) {
        if (!actor.isCustomer()) {
            throw new ForbiddenException("Chỉ khách hàng thanh toán hóa đơn của mình");
        }
        Invoice inv = invoiceService.loadVisible(actor, req.invoiceId());
        Guard.state(EnumSet.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.OVERDUE)
                .contains(inv.getStatus()), "Hóa đơn ở trạng thái \"" + inv.getStatus().getLabel() + "\" không thể thanh toán");
        BigDecimal amount = Money.of(req.amount());
        BigDecimal outstanding = inv.getTotalAmount().subtract(inv.getPaidAmount());
        Guard.input(amount.compareTo(outstanding) <= 0,
                "Số tiền vượt quá số còn phải trả (" + outstanding.toPlainString() + " " + inv.getCurrency() + ")");
        BigDecimal min = InvoiceService.minimumNextPayment(inv);
        Guard.input(amount.compareTo(min) >= 0,
                "Lần thanh toán đầu tiên tối thiểu bằng tiền đặt cọc " + min.toPlainString() + " " + inv.getCurrency());
        if (req.method() == PaymentMethod.SIMULATED_CARD) {
            Guard.input(req.cardLast4() != null, "Nhập 4 số cuối thẻ (mô phỏng)");
        }

        AppUser payer = users.getReferenceById(actor.getId());
        boolean declined = req.method() == PaymentMethod.SIMULATED_CARD && DECLINED_CARD_LAST4.equals(req.cardLast4());
        Payment p = new Payment();
        p.setPaymentReference(codes.next(CodeGenerator.PAYMENT));
        p.setInvoice(inv);
        p.setAmount(amount);
        p.setCurrency(inv.getCurrency());
        p.setMethod(req.method());
        p.setCardLast4(req.method() == PaymentMethod.SIMULATED_CARD ? req.cardLast4() : null);
        p.setPaidBy(payer);
        p.setNote(declined ? "MÔ PHỎNG: giao dịch bị từ chối (thẻ kết thúc 0002)"
                : (req.note() == null ? "Thanh toán mô phỏng" : req.note()));
        if (declined) {
            p.setStatus(PaymentStatus.FAILED);
            payments.save(p);
            return toResponse(p, inv);
        }
        p.setStatus(PaymentStatus.SUCCEEDED);
        p.setPaidAt(LocalDateTime.now(clock));
        payments.save(p);

        inv.setPaidAmount(Money.of(inv.getPaidAmount().add(amount)));
        inv.setStatus(inv.getPaidAmount().compareTo(inv.getTotalAmount()) >= 0
                ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);

        TransportationRequest r = inv.getRequest();
        if (r.getStatus() == RequestStatus.QUOTE_ACCEPTED
                && inv.getPaidAmount().compareTo(InvoiceService.depositAmount(inv)) >= 0) {
            workflow.transition(r, RequestStatus.READY_FOR_PLANNING, payer,
                    "Đã nhận thanh toán đặt cọc " + inv.getPaidAmount().toPlainString() + " " + inv.getCurrency());
            notifications.notifyRole(Role.FLEET_ROUTE_COORDINATOR, NotificationType.ACTION_REQUIRED,
                    "Sẵn sàng lập chuyến: " + r.getRequestNumber(),
                    "Khách hàng đã đặt cọc. Vui lòng chọn tuyến, phương tiện và phân công nhân sự.",
                    NotificationService.REF_REQUEST, r.getId());
        }
        notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.INFO,
                "Nhận thanh toán " + amount.toPlainString() + " " + inv.getCurrency() + " – " + inv.getInvoiceNumber(),
                "Thanh toán mô phỏng " + p.getPaymentReference() + " cho " + r.getRequestNumber(),
                NotificationService.REF_INVOICE, inv.getId());
        return toResponse(p, inv);
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(UserPrincipal actor, Long id) {
        Payment p = payments.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch"));
        invoiceService.loadVisible(actor, p.getInvoice().getId());
        return toResponse(p, p.getInvoice());
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> list(UserPrincipal actor, int page, int size) {
        if (!actor.isCustomer() && !actor.isManager()) {
            throw new ForbiddenException("Không có quyền xem giao dịch");
        }
        var result = payments.search(actor.isCustomer() ? actor.getId() : null,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result, p -> toResponse(p, p.getInvoice()));
    }

    public static PaymentResponse toResponse(Payment p, Invoice inv) {
        return new PaymentResponse(p.getId(), p.getPaymentReference(), inv.getId(), inv.getInvoiceNumber(),
                inv.getRequest().getRequestNumber(), inv.getCustomer().getFullName(), p.getAmount(), p.getCurrency(),
                p.getMethod(), p.getStatus(), p.getCardLast4(), p.getPaidAt(), p.getNote(), p.getCreatedAt());
    }
}
