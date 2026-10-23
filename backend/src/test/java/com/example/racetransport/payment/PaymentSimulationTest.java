package com.example.racetransport.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.racetransport.common.exception.BadRequestException;
import com.example.racetransport.common.exception.InvalidStateException;
import com.example.racetransport.common.repository.CodeSequenceRepository;
import com.example.racetransport.common.service.CodeGenerator;
import com.example.racetransport.notification.repository.NotificationRepository;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.payment.dto.PaymentDtos.PaymentResponse;
import com.example.racetransport.payment.dto.PaymentDtos.SimulatePaymentRequest;
import com.example.racetransport.payment.entity.Invoice;
import com.example.racetransport.payment.entity.InvoiceStatus;
import com.example.racetransport.payment.entity.Payment;
import com.example.racetransport.payment.entity.PaymentMethod;
import com.example.racetransport.payment.entity.PaymentStatus;
import com.example.racetransport.payment.repository.InvoiceRepository;
import com.example.racetransport.payment.repository.PaymentRepository;
import com.example.racetransport.payment.service.InvoiceService;
import com.example.racetransport.payment.service.PaymentService;
import com.example.racetransport.quotation.entity.Quote;
import com.example.racetransport.quotation.repository.QuoteRepository;
import com.example.racetransport.support.TestData;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.RequestStatusHistoryRepository;
import com.example.racetransport.transportationrequest.service.RequestWorkflow;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Thanh toán mô phỏng: backend quyết định số tiền hợp lệ và trạng thái hóa đơn. */
class PaymentSimulationTest {

    private PaymentRepository payments;
    private PaymentService service;
    private AppUser owner;
    private Invoice invoice;
    private TransportationRequest request;

    @BeforeEach
    void setUp() {
        Clock clock = TestData.clock();
        payments = mock(PaymentRepository.class);
        InvoiceRepository invoices = mock(InvoiceRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        CodeGenerator codes = new CodeGenerator(mock(CodeSequenceRepository.class), clock);
        InvoiceService invoiceService = new InvoiceService(invoices, payments, mock(QuoteRepository.class), codes,
                TestData.props("uploads"), clock);
        service = new PaymentService(payments, invoiceService, new RequestWorkflow(mock(RequestStatusHistoryRepository.class), clock),
                new NotificationService(mock(NotificationRepository.class), users, clock), users, codes, clock);
        owner = TestData.user(1, Role.CUSTOMER, "Owner");
        when(users.getReferenceById(1L)).thenReturn(owner);
        request = TestData.request(158, owner, RequestStatus.QUOTE_ACCEPTED);
        Quote quote = new Quote();
        quote.setId(7L);
        invoice = new Invoice();
        invoice.setId(9L);
        invoice.setInvoiceNumber("INV-2026-0009");
        invoice.setQuote(quote);
        invoice.setRequest(request);
        invoice.setCustomer(owner);
        invoice.setIssueDate(LocalDate.of(2026, 9, 30));
        invoice.setDueDate(LocalDate.of(2026, 10, 7));
        invoice.setTotalAmount(new BigDecimal("10000.00"));
        invoice.setPaidAmount(new BigDecimal("0.00"));
        invoice.setDepositPercent(new BigDecimal("50"));
        invoice.setStatus(InvoiceStatus.ISSUED);
        when(invoices.findById(9L)).thenReturn(Optional.of(invoice));
    }

    private SimulatePaymentRequest pay(String amount, PaymentMethod method, String last4) {
        return new SimulatePaymentRequest(9L, new BigDecimal(amount), method, last4, null);
    }

    @Test
    void depositMovesRequestToReadyForPlanning() {
        PaymentResponse res = service.simulate(TestData.principal(owner), pay("5000", PaymentMethod.SIMULATED_BANK_TRANSFER, null));
        assertEquals(PaymentStatus.SUCCEEDED, res.status());
        assertEquals(InvoiceStatus.PARTIALLY_PAID, invoice.getStatus());
        assertTrue(TestData.same("5000", invoice.getPaidAmount()));
        assertEquals(RequestStatus.READY_FOR_PLANNING, request.getStatus());
    }

    @Test
    void fullPaymentMarksInvoicePaid() {
        service.simulate(TestData.principal(owner), pay("10000", PaymentMethod.SIMULATED_CARD, "4242"));
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
    }

    @Test
    void cannotPayMoreThanOutstanding() {
        assertThrows(BadRequestException.class,
                () -> service.simulate(TestData.principal(owner), pay("10000.01", PaymentMethod.SIMULATED_EWALLET, null)));
        verify(payments, never()).save(any(Payment.class));
    }

    @Test
    void firstPaymentMustCoverDeposit() {
        assertThrows(BadRequestException.class,
                () -> service.simulate(TestData.principal(owner), pay("4999.99", PaymentMethod.SIMULATED_EWALLET, null)));
        assertEquals(RequestStatus.QUOTE_ACCEPTED, request.getStatus());
    }

    @Test
    void declinedCardIsRecordedAsFailedAndInvoiceUnchanged() {
        PaymentResponse res = service.simulate(TestData.principal(owner), pay("5000", PaymentMethod.SIMULATED_CARD, "0002"));
        assertEquals(PaymentStatus.FAILED, res.status());
        assertEquals(InvoiceStatus.ISSUED, invoice.getStatus());
        assertTrue(TestData.same("0", invoice.getPaidAmount()));
        assertEquals(RequestStatus.QUOTE_ACCEPTED, request.getStatus());
        verify(payments).save(any(Payment.class));
    }

    @Test
    void paidInvoiceCannotBePaidAgain() {
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAmount(new BigDecimal("10000.00"));
        assertThrows(InvalidStateException.class,
                () -> service.simulate(TestData.principal(owner), pay("1", PaymentMethod.SIMULATED_EWALLET, null)));
    }

    @Test
    void anotherCustomerCannotPay() {
        AppUser other = TestData.user(2, Role.CUSTOMER, "Other");
        assertThrows(com.example.racetransport.common.exception.ForbiddenException.class,
                () -> service.simulate(TestData.principal(other), pay("5000", PaymentMethod.SIMULATED_EWALLET, null)));
    }
}
