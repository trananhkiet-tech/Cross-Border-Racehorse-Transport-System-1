package com.example.racetransport.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.DeleteMapping;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.user.dto.UserDtos.CreateEmployeeRequest;
import com.example.racetransport.user.dto.UserDtos.EmployeeResponse;
import com.example.racetransport.user.dto.UserDtos.UpdateEmployeeStatusRequest;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Quản lý nhân viên – chỉ role nội bộ được phép. */
@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees")
public class EmployeeController {

    private final UserService userService;

    public EmployeeController(UserService userService) {
        this.userService = userService;
    }

    /** Manager và Coordinator xem danh sách (Coordinator cần để phân công Driver/Escort). */
    @GetMapping
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','FLEET_ROUTE_COORDINATOR')")
    public List<EmployeeResponse> list(@RequestParam(required = false) Role role) {
        return userService.listEmployees(role);
    }

    /** Tài xế / áp tải hợp lệ và RẢNH trong khung giờ của chuyến (loại người bị khóa, hết hạn GPLX, bận chuyến khác). */
    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','FLEET_ROUTE_COORDINATOR')")
    public List<EmployeeResponse> available(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long excludeTripId) {
        return userService.availableStaff(from, to, excludeTripId);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','FLEET_ROUTE_COORDINATOR')")
    public EmployeeResponse get(@PathVariable Long userId) {
        return userService.getEmployee(userId);
    }

    /** Chỉ Logistics Manager tạo tài khoản nhân viên. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    public EmployeeResponse create(@Valid @RequestBody CreateEmployeeRequest req) {
        return userService.createEmployee(req);
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    public EmployeeResponse setStatus(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long userId,
                                      @Valid @RequestBody UpdateEmployeeStatusRequest req) {
        return userService.setEmployeeEnabled(actor, userId, req.enabled());
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','FLEET_ROUTE_COORDINATOR')")
    @Operation(summary = "Xóa (lưu trữ) nhân sự đã nghỉ việc/bị sa thải – khóa đăng nhập, giữ lịch sử chuyến")
    public java.util.Map<String, Boolean> remove(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long userId) {
        userService.removeEmployee(actor, userId);
        return java.util.Map.of("removed", true);
    }
}
