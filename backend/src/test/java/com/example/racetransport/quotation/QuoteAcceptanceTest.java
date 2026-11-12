package com.example.racetransport.quotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.InvalidStateException;
import com.example.racetransport.common.repository.CodeSequenceRepository;
import com.example.racetransport.common.service.CodeGenerator;
import com.example.racetransport.notification.repository.NotificationRepository;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.payment.entity.Invoice;
import com.example.racetransport.payment.repository.InvoiceRepository;
import com.example.racetransport.payment.repository.PaymentRepository;
import com.example.racetransport.payment.service.InvoiceService;
import com.example.racetransport.quotation.entity.Quote;
import com.example.racetransport.quotation.entity.QuoteStatus;
import com.example.racetransport.quotation.entity.QuoteType;
import com.example.racetransport.quotation.repository.PricingRateRepository;
import com.example.racetransport.quotation.repository.QuoteItemRepository;
import com.example.racetransport.quotation.repository.QuoteRepository;
import com.example.racetransport.quotation.service.QuoteService;
import com.example.racetransport.support.TestData;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.RequestStatusHistoryRepository;
import com.example.racetransport.transportationrequest.repository.TransportationRequestRepository;
import com.example.racetransport.transportationrequest.service.RequestAccess;
import com.example.racetransport.transportationrequest.service.RequestWorkflow;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Customer không chấp nhận được báo giá đã hết hạn / không ở trạng thái cho phép / không phải của mình. */
class QuoteAcceptanceTest {

    private QuoteRepository quotes;
    private InvoiceRepository invoices;
    private QuoteService service;
    private AppUser owner;
    private TransportationRequest request;
    private Quote quote;

    @BeforeEach
    void setUp() {
        Clock clock = TestData.clock();
        quotes = mock(QuoteRepository.class);
        invoices = mock(InvoiceRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        TransportationRequestRepository requests = mock(TransportationRequestRepository.class);
        RequestAccess access = new RequestAccess(requests, mock(TripStaffAssignmentRepository.class));
        RequestWorkflow workflow = new RequestWorkflow(mock(RequestStatusHistoryRepository.class), clock);
        NotificationService notifications = new NotificationService(mock(NotificationRepository.class), users, clock);
        CodeGenerator codes = new CodeGenerator(mock(CodeSequenceRepository.class), clock);
        InvoiceService invoiceService = new InvoiceService(invoices, mock(PaymentRepository.class), quotes, codes,
                TestData.props("uploads"), clock);
        service = new QuoteService(quotes, mock(QuoteItemRepository.class), mock(PricingRateRepository.class), invoices,
                invoiceService, access, workflow, null, notifications, users, codes, TestData.props("uploads"), clock);

        owner = TestData.user(1, Role.CUSTOMER, "Owner");
        when(users.getReferenceById(1L)).thenReturn(owner);
        request = TestData.request(158, owner, RequestStatus.QUOTE_SENT);
        quote = new Quote();
        quote.setId(7L);
        quote.setQuoteNumber("QT-2026-0007");
        quote.setRequest(request);
        quote.setVersionNo(2);
        quote.setQuoteType(QuoteType.FINAL);
        quote.setStatus(QuoteStatus.SENT);
        quote.setCurrency("USD");
        quote.setTotalAmount(new BigDecimal("15900.00"));
        quote.setValidUntil(LocalDate.of(2026, 10, 10));
        quote.setCreatedBy(TestData.user(3, Role.LOGISTICS_MANAGER, "Manager"));
        when(quotes.findById(7L)).thenReturn(Optional.of(quote));
    }

    @Test
    void ownerAcceptsValidQuoteAndInvoiceIsIssued() {
        service.accept(TestData.principal(owner), 7L, null);
        assertEquals(QuoteStatus.ACCEPTED, quote.getStatus());
        assertEquals(RequestStatus.QUOTE_ACCEPTED, request.getStatus());
        verify(invoices, times(1)).save(any(Invoice.class));
    }

    @Test
    void expiredQuoteCannotBeAccepted() {
        quote.setValidUntil(LocalDate.of(2026, 9, 30));
        assertThrows(InvalidStateException.class, () -> service.accept(TestData.principal(owner), 7L, null));
        assertEquals(RequestStatus.QUOTE_SENT, request.getStatus());
        verify(invoices, never()).save(any(Invoice.class));
    }

    @Test
    void draftQuoteCannotBeAccepted() {
        quote.setStatus(QuoteStatus.DRAFT);
        assertThrows(InvalidStateException.class, () -> service.accept(TestData.principal(owner), 7L, null));
    }

    @Test
    void anotherCustomerCannotAccept() {
        AppUser other = TestData.user(2, Role.CUSTOMER, "Other");
        assertThrows(ForbiddenException.class, () -> service.accept(TestData.principal(other), 7L, null));
        assertEquals(QuoteStatus.SENT, quote.getStatus());
    }

    @Test
    void rejectRequiresReason() {
        assertThrows(com.example.racetransport.common.exception.BadRequestException.class,
                () -> service.reject(TestData.principal(owner), 7L, null));
    }
}
