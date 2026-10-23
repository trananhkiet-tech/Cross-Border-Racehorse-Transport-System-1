package com.example.racetransport.quotation.service;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.common.service.CodeGenerator;
import com.example.racetransport.common.util.Countries;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.common.util.Money;
import com.example.racetransport.config.AppProperties;
import com.example.racetransport.notification.entity.NotificationType;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.payment.entity.InvoiceStatus;
import com.example.racetransport.payment.repository.InvoiceRepository;
import com.example.racetransport.payment.service.InvoiceService;
import com.example.racetransport.quotation.dto.QuoteDtos.PricingRateResponse;
import com.example.racetransport.quotation.dto.QuoteDtos.QuoteItemInput;
import com.example.racetransport.quotation.dto.QuoteDtos.QuoteItemResponse;
import com.example.racetransport.quotation.dto.QuoteDtos.QuoteResponse;
import com.example.racetransport.quotation.dto.QuoteDtos.QuoteUpdateRequest;
import com.example.racetransport.quotation.dto.QuoteDtos.RespondRequest;
import com.example.racetransport.quotation.entity.Quote;
import com.example.racetransport.quotation.entity.QuoteItem;
import com.example.racetransport.quotation.entity.QuoteStatus;
import com.example.racetransport.quotation.entity.QuoteType;
import com.example.racetransport.quotation.repository.PricingRateRepository;
import com.example.racetransport.quotation.repository.QuoteItemRepository;
import com.example.racetransport.quotation.repository.QuoteRepository;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.service.RequestAccess;
import com.example.racetransport.transportationrequest.service.RequestWorkflow;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Final Quote: Estimated Cost → Manager Review (chỉnh + xác nhận từng dòng) → gửi khách →
 * Customer Accept/Reject. Mỗi lần sửa sau khi đã gửi = phiên bản mới (không ghi đè báo giá đã gửi).
 */
@Service
public class QuoteService {

    private static final EnumSet<RequestStatus> QUOTABLE = EnumSet.of(RequestStatus.APPROVED, RequestStatus.QUOTE_SENT,
            RequestStatus.QUOTE_REJECTED);

    private final QuoteRepository quotes;
    private final QuoteItemRepository quoteItems;
    private final PricingRateRepository rates;
    private final InvoiceRepository invoices;
    private final InvoiceService invoiceService;
    private final RequestAccess requestAccess;
    private final RequestWorkflow workflow;
    private final EstimateService estimateService;
    private final NotificationService notifications;
    private final AppUserRepository users;
    private final CodeGenerator codes;
    private final AppProperties props;
    private final Clock clock;

    public QuoteService(QuoteRepository quotes, QuoteItemRepository quoteItems, PricingRateRepository rates,
                        InvoiceRepository invoices, InvoiceService invoiceService, RequestAccess requestAccess,
                        RequestWorkflow workflow, EstimateService estimateService, NotificationService notifications,
                        AppUserRepository users, CodeGenerator codes, AppProperties props, Clock clock) {
        this.quotes = quotes;
        this.quoteItems = quoteItems;
        this.rates = rates;
        this.invoices = invoices;
        this.invoiceService = invoiceService;
        this.requestAccess = requestAccess;
        this.workflow = workflow;
        this.estimateService = estimateService;
        this.notifications = notifications;
        this.users = users;
        this.codes = codes;
        this.props = props;
        this.clock = clock;
    }

    // ================================================================== queries
    @Transactional(readOnly = true)
    public QuoteResponse get(UserPrincipal actor, Long id) {
        Quote q = loadVisible(actor, id);
        return toResponse(actor, q);
    }

    @Transactional(readOnly = true)
    public List<QuoteResponse> listForRequest(UserPrincipal actor, Long requestId) {
        requestAccess.loadVisible(actor, requestId);
        Guard.state(!actor.isDriverEscort(), "Không có quyền xem báo giá");
        return quotes.findByRequestIdOrderByVersionNoDesc(requestId).stream()
                .filter(q -> canView(actor, q))
                .map(q -> toResponse(actor, q)).toList();
    }

    /** Danh sách báo giá cuối (Quote Management). Customer: chỉ báo giá đã gửi cho mình. */
    @Transactional(readOnly = true)
    public PageResponse<QuoteResponse> listFinal(UserPrincipal actor, Collection<QuoteStatus> statuses, int page,
                                                 int size) {
        if (actor.isDriverEscort() || actor.isSpecialist() || actor.isCoordinator()) {
            throw new ForbiddenException("Không có quyền xem danh sách báo giá");
        }
        Collection<QuoteStatus> filter = statuses == null || statuses.isEmpty()
                ? EnumSet.of(QuoteStatus.DRAFT, QuoteStatus.SENT, QuoteStatus.ACCEPTED, QuoteStatus.REJECTED,
                QuoteStatus.EXPIRED, QuoteStatus.SUPERSEDED) : statuses;
        if (actor.isCustomer()) {
            filter = filter.stream().filter(s -> s != QuoteStatus.DRAFT).toList();
            if (filter.isEmpty()) {
                filter = EnumSet.of(QuoteStatus.SENT);
            }
        }
        var result = quotes.searchFinal(actor.isCustomer() ? actor.getId() : null, filter,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "updatedAt")));
        return PageResponse.of(result, q -> toResponse(actor, q));
    }

    @Transactional(readOnly = true)
    public List<PricingRateResponse> pricingRates() {
        return rates.findAllByOrderByCategoryAscCodeAsc().stream()
                .map(p -> new PricingRateResponse(p.getId(), p.getCode(), p.getCategory(), p.getDescription(),
                        p.getUnit(), p.getUnitPrice(), p.getCurrency(), p.getTransportMode(),
                        p.getDestinationCountry(), Boolean.TRUE.equals(p.getActive())))
                .toList();
    }

    // ================================================================== manager actions
    /** Tạo bản nháp báo giá cuối từ báo giá cuối gần nhất (nếu có) hoặc từ chi phí ước tính. */
    @Transactional
    public QuoteResponse createFinal(UserPrincipal actor, Long requestId) {
        requireManager(actor);
        TransportationRequest r = requestAccess.load(requestId);
        Guard.state(QUOTABLE.contains(r.getStatus()),
                "Chỉ lập báo giá cuối cho yêu cầu đã phê duyệt (trạng thái hiện tại: " + r.getStatus().getLabel() + ")");
        var existingDraft = quotes.findByRequestIdAndStatusIn(r.getId(), EnumSet.of(QuoteStatus.DRAFT)).stream()
                .filter(q -> q.getQuoteType() == QuoteType.FINAL).findFirst();
        if (existingDraft.isPresent()) {
            return toResponse(actor, existingDraft.get());
        }
        Quote base = quotes.findFirstByRequestIdAndQuoteTypeOrderByVersionNoDesc(r.getId(), QuoteType.FINAL)
                .or(() -> quotes.findFirstByRequestIdAndQuoteTypeOrderByVersionNoDesc(r.getId(), QuoteType.ESTIMATE))
                .orElseGet(() -> estimateService.generateEstimate(r, users.getReferenceById(actor.getId())));
        Quote estimate = quotes.findFirstByRequestIdAndQuoteTypeOrderByVersionNoDesc(r.getId(), QuoteType.ESTIMATE)
                .orElse(base);

        Quote q = new Quote();
        q.setRequest(r);
        q.setQuoteNumber(codes.next(CodeGenerator.QUOTE));
        q.setVersionNo(estimateService.nextVersion(r.getId()));
        q.setQuoteType(QuoteType.FINAL);
        q.setStatus(QuoteStatus.DRAFT);
        q.setCurrency(base.getCurrency());
        q.setEstimatedTotal(estimate.getTotalAmount());
        q.setValidUntil(LocalDate.now(clock).plusDays(props.business().quoteValidityDays()));
        q.setCreatedBy(users.getReferenceById(actor.getId()));
        q.setNotes(base.getQuoteType() == QuoteType.FINAL ? base.getNotes() : null);
        List<QuoteItem> copies = new ArrayList<>();
        for (QuoteItem src : quoteItems.findByQuoteIdOrderBySortOrderAscIdAsc(base.getId())) {
            QuoteItem it = new QuoteItem();
            it.setCategory(src.getCategory());
            it.setDescription(src.getDescription());
            it.setQuantity(src.getQuantity());
            it.setUnitPrice(src.getUnitPrice());
            it.setAmount(src.getAmount());
            it.setEstimatedAmount(src.getEstimatedAmount() != null ? src.getEstimatedAmount() : src.getAmount());
            it.setConfirmed(false);
            it.setSource(src.getSource());
            it.setSortOrder(src.getSortOrder());
            copies.add(it);
        }
        EstimateService.applyTotals(q, copies);
        quotes.save(q);
        copies.forEach(it -> {
            it.setQuote(q);
            quoteItems.save(it);
        });
        return toResponse(actor, q);
    }

    /** Sửa bản nháp: thay toàn bộ dòng chi phí, backend tính lại tổng. */
    @Transactional
    public QuoteResponse update(UserPrincipal actor, Long id, QuoteUpdateRequest req) {
        requireManager(actor);
        Quote q = load(id);
        Guard.state(q.getQuoteType() == QuoteType.FINAL && q.getStatus() == QuoteStatus.DRAFT,
                "Chỉ sửa được báo giá cuối ở trạng thái Nháp; báo giá đã gửi cần tạo phiên bản mới");
        if (req.validUntil() != null) {
            Guard.input(!req.validUntil().isBefore(LocalDate.now(clock)), "Hạn hiệu lực phải từ hôm nay trở đi");
            q.setValidUntil(req.validUntil());
        }
        q.setNotes(req.notes());
        quoteItems.deleteByQuoteId(q.getId());
        quoteItems.flush();
        List<QuoteItem> lines = new ArrayList<>();
        int order = 0;
        for (QuoteItemInput in : req.items()) {
            QuoteItem it = new QuoteItem();
            it.setQuote(q);
            it.setCategory(in.category());
            it.setDescription(in.description().trim());
            it.setQuantity(in.quantity());
            it.setUnitPrice(Money.of(in.unitPrice()));
            it.setAmount(QuoteCalculator.lineAmount(in.category(), in.quantity(), it.getUnitPrice()));
            it.setEstimatedAmount(in.estimatedAmount());
            it.setConfirmed(Boolean.TRUE.equals(in.confirmed()));
            it.setSource(in.source());
            it.setSortOrder(order++);
            lines.add(it);
        }
        EstimateService.applyTotals(q, lines);
        lines.forEach(quoteItems::save);
        return toResponse(actor, q);
    }

    /** Gửi báo giá cho khách: mọi dòng phải được xác nhận; phiên bản đã gửi trước đó chuyển SUPERSEDED. */
    @Transactional
    public QuoteResponse send(UserPrincipal actor, Long id) {
        requireManager(actor);
        Quote q = load(id);
        TransportationRequest r = q.getRequest();
        Guard.state(q.getQuoteType() == QuoteType.FINAL && q.getStatus() == QuoteStatus.DRAFT,
                "Chỉ gửi được báo giá cuối ở trạng thái Nháp");
        Guard.state(QUOTABLE.contains(r.getStatus()), "Yêu cầu không ở trạng thái cho phép gửi báo giá");
        List<QuoteItem> lines = quoteItems.findByQuoteIdOrderBySortOrderAscIdAsc(q.getId());
        Guard.state(!lines.isEmpty(), "Báo giá chưa có dòng chi phí");
        long unconfirmed = lines.stream().filter(i -> !Boolean.TRUE.equals(i.getConfirmed())).count();
        Guard.state(unconfirmed == 0, "Còn " + unconfirmed + " dòng chi phí chưa được xác nhận");
        EstimateService.applyTotals(q, lines); // tính lại lần cuối ở server
        Guard.state(q.getTotalAmount().signum() > 0, "Tổng báo giá phải lớn hơn 0");
        if (q.getValidUntil() == null || q.getValidUntil().isBefore(LocalDate.now(clock))) {
            q.setValidUntil(LocalDate.now(clock).plusDays(props.business().quoteValidityDays()));
        }
        quotes.findByRequestIdAndStatusIn(r.getId(), EnumSet.of(QuoteStatus.SENT))
                .forEach(old -> old.setStatus(QuoteStatus.SUPERSEDED));
        q.setStatus(QuoteStatus.SENT);
        q.setSentAt(LocalDateTime.now(clock));
        AppUser manager = users.getReferenceById(actor.getId());
        workflow.transition(r, RequestStatus.QUOTE_SENT, manager,
                "Gửi báo giá " + q.getQuoteNumber() + " (phiên bản " + q.getVersionNo() + ")");
        notifications.notify(r.getCustomer(), NotificationType.ACTION_REQUIRED,
                "Báo giá cuối cho " + r.getRequestNumber(),
                "Tổng " + q.getTotalAmount().toPlainString() + " " + q.getCurrency() + ", hiệu lực đến "
                        + q.getValidUntil() + ". Vui lòng chấp nhận hoặc từ chối.",
                NotificationService.REF_QUOTE, q.getId());
        return toResponse(actor, q);
    }

    // ================================================================== customer actions
    @Transactional
    public QuoteResponse accept(UserPrincipal actor, Long id, RespondRequest body) {
        Quote q = loadForCustomerResponse(actor, id);
        AppUser customer = users.getReferenceById(actor.getId());
        q.setStatus(QuoteStatus.ACCEPTED);
        q.setAcceptedAt(LocalDateTime.now(clock));
        q.setCustomerResponseNote(body == null ? null : body.note());
        workflow.transition(q.getRequest(), RequestStatus.QUOTE_ACCEPTED, customer,
                "Khách hàng chấp nhận báo giá " + q.getQuoteNumber());
        invoiceService.createForQuote(q);
        notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.SUCCESS,
                "Khách hàng chấp nhận báo giá " + q.getQuoteNumber(),
                q.getRequest().getRequestNumber() + " – hóa đơn đã được phát hành, chờ thanh toán đặt cọc.",
                NotificationService.REF_QUOTE, q.getId());
        return toResponse(actor, q);
    }

    @Transactional
    public QuoteResponse reject(UserPrincipal actor, Long id, RespondRequest body) {
        Guard.input(body != null && body.note() != null && !body.note().isBlank(), "Vui lòng cho biết lý do từ chối");
        Quote q = loadForCustomerResponse(actor, id);
        q.setStatus(QuoteStatus.REJECTED);
        q.setRejectedAt(LocalDateTime.now(clock));
        q.setCustomerResponseNote(body.note());
        workflow.transition(q.getRequest(), RequestStatus.QUOTE_REJECTED, users.getReferenceById(actor.getId()),
                "Khách hàng từ chối báo giá: " + body.note());
        notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.WARNING,
                "Khách hàng từ chối báo giá " + q.getQuoteNumber(), body.note(), NotificationService.REF_QUOTE,
                q.getId());
        return toResponse(actor, q);
    }

    /** Kiểm tra: đúng chủ sở hữu, báo giá cuối đang SENT và còn hiệu lực (hết hạn → 409). */
    private Quote loadForCustomerResponse(UserPrincipal actor, Long id) {
        Quote q = load(id);
        if (!requestAccess.isOwner(actor, q.getRequest())) {
            throw new ForbiddenException("Bạn không có quyền phản hồi báo giá này");
        }
        Guard.state(q.getQuoteType() == QuoteType.FINAL, "Chi phí ước tính không cần chấp nhận");
        Guard.state(q.getStatus() == QuoteStatus.SENT,
                "Báo giá ở trạng thái \"" + q.getStatus().getLabel() + "\" không thể phản hồi");
        Guard.state(q.getRequest().getStatus() == RequestStatus.QUOTE_SENT, "Yêu cầu không còn chờ phản hồi báo giá");
        Guard.state(q.getValidUntil() == null || !q.getValidUntil().isBefore(LocalDate.now(clock)),
                "Báo giá đã hết hạn ngày " + q.getValidUntil() + ", vui lòng liên hệ để nhận báo giá mới");
        return q;
    }

    // ================================================================== helpers
    public Quote load(Long id) {
        return quotes.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy báo giá"));
    }

    private Quote loadVisible(UserPrincipal actor, Long id) {
        Quote q = load(id);
        if (!canView(actor, q)) {
            throw new ForbiddenException("Bạn không có quyền xem báo giá này");
        }
        return q;
    }

    private boolean canView(UserPrincipal actor, Quote q) {
        if (actor.isDriverEscort()) {
            return false;
        }
        if (actor.isCustomer()) {
            return requestAccess.isOwner(actor, q.getRequest())
                    && (q.getQuoteType() == QuoteType.ESTIMATE || q.getSentAt() != null);
        }
        return true;
    }

    private static void requireManager(UserPrincipal actor) {
        if (!actor.isManager()) {
            throw new ForbiddenException("Chỉ Logistics Manager quản lý báo giá cuối");
        }
    }

    private QuoteResponse toResponse(UserPrincipal actor, Quote q) {
        TransportationRequest r = q.getRequest();
        List<QuoteItemResponse> items = quoteItems.findByQuoteIdOrderBySortOrderAscIdAsc(q.getId()).stream()
                .map(i -> new QuoteItemResponse(i.getId(), i.getCategory(), i.getDescription(), i.getQuantity(),
                        i.getUnitPrice(), i.getAmount(), i.getEstimatedAmount(), Boolean.TRUE.equals(i.getConfirmed()),
                        i.getSource(), i.getSortOrder()))
                .toList();
        boolean expired = q.getStatus() == QuoteStatus.EXPIRED || (q.getStatus() == QuoteStatus.SENT
                && q.getValidUntil() != null && q.getValidUntil().isBefore(LocalDate.now(clock)));
        Long invoiceId = invoices.findByQuoteId(q.getId()).filter(i -> i.getStatus() != InvoiceStatus.CANCELLED)
                .map(i -> i.getId()).orElse(null);
        List<String> actions = new ArrayList<>();
        if (actor.isManager() && q.getQuoteType() == QuoteType.FINAL && q.getStatus() == QuoteStatus.DRAFT) {
            actions.add("EDIT");
            actions.add("SEND");
        }
        if (actor.isManager() && QUOTABLE.contains(r.getStatus())
                && (q.getStatus() == QuoteStatus.SENT || q.getStatus() == QuoteStatus.REJECTED
                || q.getStatus() == QuoteStatus.EXPIRED || q.getQuoteType() == QuoteType.ESTIMATE)) {
            actions.add("NEW_VERSION");
        }
        if (requestAccess.isOwner(actor, r) && q.getQuoteType() == QuoteType.FINAL && q.getStatus() == QuoteStatus.SENT
                && !expired && r.getStatus() == RequestStatus.QUOTE_SENT) {
            actions.add("ACCEPT");
            actions.add("REJECT");
        }
        return new QuoteResponse(q.getId(), q.getQuoteNumber(), r.getId(), r.getRequestNumber(), r.getStatus(),
                r.getCustomer().getId(), r.getCustomer().getFullName(),
                Countries.name(r.getOriginCountry()) + " → " + Countries.name(r.getDestinationCountry()),
                q.getVersionNo(), q.getQuoteType(), q.getStatus(), q.getCurrency(), q.getSubtotal(),
                q.getDiscountTotal(), q.getTotalAmount(), q.getEstimatedTotal(), r.getDeclaredValueTotal(),
                q.getValidUntil(), expired, q.getNotes(), q.getCreatedBy().getFullName(), q.getCreatedAt(),
                q.getUpdatedAt(), q.getSentAt(), q.getAcceptedAt(), q.getRejectedAt(), q.getCustomerResponseNote(),
                invoiceId, items, actions);
    }
}
