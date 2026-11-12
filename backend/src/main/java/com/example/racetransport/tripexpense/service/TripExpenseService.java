package com.example.racetransport.tripexpense.service;

import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.notification.entity.NotificationType;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import com.example.racetransport.trip.service.TripAccess;
import com.example.racetransport.tripexpense.dto.ExpenseDtos.ExpenseRequest;
import com.example.racetransport.tripexpense.dto.ExpenseDtos.ExpenseResponse;
import com.example.racetransport.tripexpense.dto.ExpenseDtos.ReviewRequest;
import com.example.racetransport.tripexpense.entity.ExpenseStatus;
import com.example.racetransport.tripexpense.entity.TripExpense;
import com.example.racetransport.tripexpense.repository.TripExpenseRepository;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chi phí phát sinh: Driver/Escort được phân công chuyến ghi chi phí (chuyến chưa hủy), Manager/Coordinator duyệt.
 * Từ chối bắt buộc có lý do. Chi phí đã duyệt/từ chối thì không đổi nữa.
 */
@Service
public class TripExpenseService {

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("100000");

    private final TripExpenseRepository expenses;
    private final TripAccess tripAccess;
    private final TransportationTripRepository trips;
    private final AppUserRepository users;
    private final NotificationService notifications;
    private final Clock clock;

    public TripExpenseService(TripExpenseRepository expenses, TripAccess tripAccess, TransportationTripRepository trips,
                              AppUserRepository users, NotificationService notifications, Clock clock) {
        this.expenses = expenses;
        this.tripAccess = tripAccess;
        this.trips = trips;
        this.users = users;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public ExpenseResponse create(UserPrincipal actor, Long tripId, ExpenseRequest req) {
        TransportationTrip trip = tripAccess.loadAssigned(actor, tripId);
        Guard.state(trip.getStatus() != TripStatus.CANCELLED, "Chuyến đã hủy, không ghi thêm chi phí");
        Guard.input(req.amount().compareTo(MAX_AMOUNT) <= 0, "Số tiền vượt giới hạn cho phép");
        TripExpense e = new TripExpense();
        e.setTripId(trip.getId());
        e.setStaffId(actor.getId());
        e.setCategory(req.category());
        e.setAmount(req.amount());
        e.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().trim());
        e.setCreatedAt(LocalDateTime.now(clock));
        e = expenses.save(e);
        notifications.notifyRole(Role.FLEET_ROUTE_COORDINATOR, NotificationType.ACTION_REQUIRED,
                "Chi phí chuyến " + trip.getTripNumber() + " chờ duyệt",
                actor.getFullName() + " ghi " + e.getAmount() + " " + e.getCurrency() + " (" + e.getCategory().getLabel() + ")",
                NotificationService.REF_TRIP, trip.getId());
        return toResponse(e, trip, null);
    }

    @Transactional
    public ExpenseResponse review(UserPrincipal actor, Long id, ReviewRequest req) {
        TripExpense e = expenses.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy chi phí"));
        Guard.state(e.getStatus() == ExpenseStatus.PENDING, "Chi phí đã được xử lý");
        boolean approve = Boolean.TRUE.equals(req.approve());
        String note = req.note() == null ? "" : req.note().trim();
        Guard.input(approve || !note.isEmpty(), "Từ chối phải nhập lý do");
        e.setStatus(approve ? ExpenseStatus.APPROVED : ExpenseStatus.REJECTED);
        e.setReviewNote(note.isEmpty() ? null : note);
        e.setReviewedBy(actor.getId());
        e.setReviewedAt(LocalDateTime.now(clock));
        expenses.save(e);
        AppUser staff = users.findById(e.getStaffId()).orElse(null);
        TransportationTrip trip = trips.findById(e.getTripId()).orElse(null);
        notifications.notify(staff, approve ? NotificationType.SUCCESS : NotificationType.WARNING,
                approve ? "Chi phí đã được duyệt hoàn ứng" : "Chi phí bị từ chối",
                e.getAmount() + " " + e.getCurrency() + (note.isEmpty() ? "" : " — " + note),
                NotificationService.REF_TRIP, e.getTripId());
        return toResponse(e, trip, staff);
    }

    /** Tài xế/áp tải: chi phí của mình; Manager/Coordinator: tất cả hoặc theo trạng thái. */
    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(UserPrincipal actor, ExpenseStatus status) {
        List<TripExpense> rows;
        if (actor.isDriverEscort()) {
            rows = expenses.findByStaffIdOrderByCreatedAtDesc(actor.getId());
            if (status != null) {
                rows = rows.stream().filter(x -> x.getStatus() == status).toList();
            }
        } else {
            rows = status == null ? expenses.findAllByOrderByCreatedAtDesc() : expenses.findByStatusOrderByCreatedAtAsc(status);
        }
        return rows.stream().map(e -> toResponse(e, trips.findById(e.getTripId()).orElse(null),
                users.findById(e.getStaffId()).orElse(null))).toList();
    }

    static ExpenseResponse toResponse(TripExpense e, TransportationTrip trip, AppUser staff) {
        return new ExpenseResponse(e.getId(), e.getTripId(), trip == null ? null : trip.getTripNumber(), e.getStaffId(),
                staff == null ? null : staff.getFullName(), e.getCategory(), e.getAmount(), e.getCurrency(),
                e.getDescription(), e.getStatus(), e.getReviewNote(), e.getReviewedAt(), e.getCreatedAt());
    }
}
