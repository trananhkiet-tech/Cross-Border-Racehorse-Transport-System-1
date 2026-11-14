package com.example.racetransport.staffleave.service;

import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.notification.entity.NotificationType;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.staffleave.dto.LeaveDtos.LeaveRequest;
import com.example.racetransport.staffleave.dto.LeaveDtos.LeaveResponse;
import com.example.racetransport.staffleave.entity.LeaveStatus;
import com.example.racetransport.staffleave.entity.StaffLeave;
import com.example.racetransport.staffleave.repository.StaffLeaveRepository;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lịch nghỉ phép tài xế/áp tải do Coordinator/Manager nhập. Không cho nghỉ trùng kỳ nghỉ khác
 * hoặc trùng chuyến đang được phân công (phải đổi người trước).
 */
@Service
public class StaffLeaveService {

    private final StaffLeaveRepository leaves;
    private final AppUserRepository users;
    private final TripStaffAssignmentRepository assignments;
    private final NotificationService notifications;
    private final Clock clock;

    public StaffLeaveService(StaffLeaveRepository leaves, AppUserRepository users,
                             TripStaffAssignmentRepository assignments, NotificationService notifications, Clock clock) {
        this.leaves = leaves;
        this.users = users;
        this.assignments = assignments;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public LeaveResponse create(UserPrincipal actor, LeaveRequest req) {
        Guard.input(!req.endDate().isBefore(req.startDate()), "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        Guard.input(!req.endDate().isBefore(LocalDate.now(clock)), "Không thể tạo kỳ nghỉ đã qua");
        AppUser staff = users.findById(req.staffId()).orElseThrow(() -> new NotFoundException("Không tìm thấy nhân sự"));
        Guard.input(staff.getRole() == Role.DRIVER_ESCORT, "Chỉ tạo lịch nghỉ cho tài xế / người áp tải");
        Guard.state(!leaves.existsOverlap(staff.getId(), req.startDate(), req.endDate()),
                "Nhân sự đã có kỳ nghỉ trùng khoảng ngày này");
        Guard.state(!assignments.existsStaffOverlap(staff.getId(), -1L, req.startDate().atStartOfDay(),
                        req.endDate().plusDays(1).atStartOfDay()),
                "Nhân sự đang được phân công chuyến trong khoảng này, hãy đổi người trước");
        StaffLeave l = new StaffLeave();
        l.setStaffId(staff.getId());
        l.setStartDate(req.startDate());
        l.setEndDate(req.endDate());
        l.setReason(req.reason() == null || req.reason().isBlank() ? null : req.reason().trim());
        l.setCreatedBy(actor.getId());
        l.setCreatedAt(LocalDateTime.now(clock));
        l = leaves.save(l);
        notifications.notify(staff, NotificationType.INFO, "Lịch nghỉ phép đã được ghi nhận",
                "Từ " + l.getStartDate() + " đến " + l.getEndDate(), null, null);
        return toResponse(l, staff);
    }

    @Transactional
    public LeaveResponse cancel(UserPrincipal actor, Long id) {
        StaffLeave l = leaves.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ nghỉ"));
        Guard.state(l.getStatus() == LeaveStatus.ACTIVE, "Kỳ nghỉ đã bị hủy");
        l.setStatus(LeaveStatus.CANCELLED);
        leaves.save(l);
        return toResponse(l, users.findById(l.getStaffId()).orElse(null));
    }

    /** Các kỳ nghỉ chưa kết thúc (kể cả đang diễn ra). */
    @Transactional(readOnly = true)
    public List<LeaveResponse> upcoming() {
        return leaves.findByStatusAndEndDateGreaterThanEqualOrderByStartDateAsc(LeaveStatus.ACTIVE, LocalDate.now(clock))
                .stream().map(l -> toResponse(l, users.findById(l.getStaffId()).orElse(null))).toList();
    }

    /** Tài xế/áp tải xem lịch nghỉ của chính mình. */
    @Transactional(readOnly = true)
    public List<LeaveResponse> mine(UserPrincipal actor) {
        AppUser me = users.findById(actor.getId()).orElse(null);
        return leaves.findByStaffIdOrderByStartDateDesc(actor.getId()).stream().map(l -> toResponse(l, me)).toList();
    }

    /** Nhân sự có đang nghỉ trong ngày không (dùng để lọc khi phân công). */
    @Transactional(readOnly = true)
    public boolean isOnLeave(Long staffId, LocalDate date) {
        return leaves.existsOverlap(staffId, date, date);
    }

    static LeaveResponse toResponse(StaffLeave l, AppUser staff) {
        return new LeaveResponse(l.getId(), l.getStaffId(), staff == null ? "—" : staff.getFullName(),
                l.getStartDate(), l.getEndDate(), l.getReason(), l.getStatus());
    }
}
