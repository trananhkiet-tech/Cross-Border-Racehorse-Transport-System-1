package com.example.racetransport.user.service;

import com.example.racetransport.common.exception.BadRequestException;
import com.example.racetransport.common.exception.ConflictException;
import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.util.Countries;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.repository.TripStaffAssignmentRepository;
import com.example.racetransport.user.dto.UserDtos.ChangePasswordRequest;
import com.example.racetransport.user.dto.UserDtos.CreateEmployeeRequest;
import com.example.racetransport.user.dto.UserDtos.CustomerProfileResponse;
import com.example.racetransport.user.dto.UserDtos.EmployeeResponse;
import com.example.racetransport.user.dto.UserDtos.UpdateCustomerProfileRequest;
import com.example.racetransport.user.dto.UserDtos.UpdateMyAccountRequest;
import com.example.racetransport.user.dto.UserDtos.UserSummary;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.CustomerProfile;
import com.example.racetransport.user.entity.CustomerType;
import com.example.racetransport.user.entity.EmployeeProfile;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.entity.StaffRole;
import com.example.racetransport.user.repository.AppUserRepository;
import com.example.racetransport.user.repository.CustomerProfileRepository;
import com.example.racetransport.user.repository.EmployeeProfileRepository;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Hồ sơ khách hàng, tài khoản cá nhân và quản lý nhân viên. */
@Service
public class UserService {

    /** Trạng thái chuyến được coi là "đang được giao" khi đếm phân công. */
    public static final EnumSet<TripStatus> ACTIVE_TRIP_STATUSES = EnumSet.complementOf(
            EnumSet.of(TripStatus.DELIVERED, TripStatus.CANCELLED));

    private final AppUserRepository users;
    private final CustomerProfileRepository customers;
    private final EmployeeProfileRepository employees;
    private final TripStaffAssignmentRepository assignments;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository users, CustomerProfileRepository customers,
                       EmployeeProfileRepository employees, TripStaffAssignmentRepository assignments,
                       PasswordEncoder passwordEncoder) {
        this.users = users;
        this.customers = customers;
        this.employees = employees;
        this.assignments = assignments;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser getUser(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
    }

    @Transactional(readOnly = true)
    public UserSummary me(UserPrincipal actor) {
        return toSummary(getUser(actor.getId()));
    }

    // ------------------------------------------------------------- customer profile
    @Transactional
    public CustomerProfileResponse getCustomerProfile(UserPrincipal actor) {
        requireCustomer(actor);
        AppUser user = getUser(actor.getId());
        return toCustomerResponse(user, profileOf(user));
    }

    @Transactional
    public CustomerProfileResponse updateCustomerProfile(UserPrincipal actor, UpdateCustomerProfileRequest req) {
        requireCustomer(actor);
        if (req.country() != null && !req.country().isBlank()) {
            Guard.input(Countries.isSupported(req.country()), "Mã quốc gia không được hỗ trợ");
        }
        AppUser user = getUser(actor.getId());
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone());
        CustomerProfile profile = profileOf(user);
        profile.setCompanyName(req.companyName());
        profile.setCustomerType(req.customerType());
        profile.setAddress(req.address());
        profile.setCountry(req.country());
        profile.setTaxCode(req.taxCode());
        if (req.preferredLanguage() != null) {
            profile.setPreferredLanguage(req.preferredLanguage());
        }
        return toCustomerResponse(user, profile);
    }

    /** Tạo profile nếu dữ liệu cũ chưa có (an toàn khi tài khoản được tạo bằng script). */
    private CustomerProfile profileOf(AppUser user) {
        return customers.findByUserId(user.getId()).orElseGet(() -> {
            CustomerProfile p = new CustomerProfile();
            p.setUser(user);
            p.setCustomerType(CustomerType.INDIVIDUAL);
            return customers.save(p);
        });
    }

    // ------------------------------------------------------------- account (mọi role)
    @Transactional
    public UserSummary updateMyAccount(UserPrincipal actor, UpdateMyAccountRequest req) {
        AppUser user = getUser(actor.getId());
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone());
        return toSummary(user);
    }

    @Transactional
    public void changePassword(UserPrincipal actor, ChangePasswordRequest req) {
        AppUser user = getUser(actor.getId());
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng");
        }
        Guard.input(!req.currentPassword().equals(req.newPassword()), "Mật khẩu mới phải khác mật khẩu cũ");
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
    }

    // ------------------------------------------------------------- employees
    @Transactional(readOnly = true)
    public List<EmployeeResponse> listEmployees(Role role) {
        List<AppUser> list = new ArrayList<>();
        if (role != null) {
            Guard.input(role != Role.CUSTOMER, "Danh sách nhân viên không bao gồm khách hàng");
            list.addAll(users.findByRoleOrderByFullNameAsc(role));
        } else {
            for (Role r : Role.values()) {
                if (r != Role.CUSTOMER) {
                    list.addAll(users.findByRoleOrderByFullNameAsc(r));
                }
            }
        }
        Map<Long, EmployeeProfile> profiles = employees.findByUserIdIn(list.stream().map(AppUser::getId).toList())
                .stream().collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity()));
        return list.stream().filter(u -> !isArchived(profiles.get(u.getId())))
                .map(u -> toEmployeeResponse(u, profiles.get(u.getId()))).toList();
    }

    private static boolean isArchived(EmployeeProfile p) {
        return p != null && Boolean.TRUE.equals(p.getArchived());
    }

    /**
     * Nhân sự CÓ THỂ phân công cho khung giờ [from, to]: tài khoản DRIVER_ESCORT đang hoạt động, có hồ sơ chuyên môn,
     * tài xế phải có giấy phép còn hạn đến hết chuyến, và không bận chuyến khác trùng khung giờ.
     * Người không hợp lệ / đang bận bị loại khỏi danh sách (excludeTripId = chuyến đang chỉnh, được bỏ qua khi xét trùng lịch).
     */
    @Transactional(readOnly = true)
    public List<EmployeeResponse> availableStaff(LocalDateTime from, LocalDateTime to, Long excludeTripId) {
        Guard.input(from != null && to != null && to.isAfter(from), "Khung giờ không hợp lệ");
        long tripId = excludeTripId == null ? -1L : excludeTripId;
        List<AppUser> list = users.findByRoleOrderByFullNameAsc(Role.DRIVER_ESCORT).stream()
                .filter(u -> Boolean.TRUE.equals(u.getEnabled())).toList();
        Map<Long, EmployeeProfile> profiles = employees.findByUserIdIn(list.stream().map(AppUser::getId).toList())
                .stream().collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity(), (a, b) -> a));
        return list.stream()
                .filter(u -> {
                    EmployeeProfile p = profiles.get(u.getId());
                    if (p == null || p.getStaffRole() == null || isArchived(p)) {
                        return false;
                    }
                    if (p.getStaffRole() == StaffRole.DRIVER
                            && (p.getLicenseNumber() == null || p.getLicenseNumber().isBlank()
                            || (p.getLicenseExpiry() != null && p.getLicenseExpiry().isBefore(to.toLocalDate())))) {
                        return false;
                    }
                    return !assignments.existsStaffOverlap(u.getId(), tripId, from, to);
                })
                .map(u -> toEmployeeResponse(u, profiles.get(u.getId()))).toList();
    }

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest req) {
        Guard.input(req.role() != Role.CUSTOMER, "Không tạo tài khoản khách hàng tại đây");
        if (req.role() == Role.DRIVER_ESCORT) {
            Guard.input(req.staffRole() != null, "Cần chọn vai trò chuyên môn cho tài xế/áp tải");
        }
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email đã được sử dụng");
        }
        if (employees.existsByEmployeeCode(req.employeeCode())) {
            throw new ConflictException("Mã nhân viên đã tồn tại");
        }
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone());
        user.setRole(req.role());
        user.setEnabled(true);
        user.setPasswordHash(passwordEncoder.encode(req.initialPassword()));
        users.save(user);
        EmployeeProfile p = new EmployeeProfile();
        p.setUser(user);
        p.setEmployeeCode(req.employeeCode());
        p.setDepartment(req.department());
        p.setStaffRole(req.role() == Role.DRIVER_ESCORT ? req.staffRole() : null);
        p.setLicenseNumber(req.licenseNumber());
        p.setLicenseExpiry(req.licenseExpiry());
        p.setAirEscortCertified(Boolean.TRUE.equals(req.airEscortCertified()));
        p.setEmergencyContact(req.emergencyContact());
        p.setBaseLocation(req.baseLocation());
        employees.save(p);
        return toEmployeeResponse(user, p);
    }

    @Transactional
    public EmployeeResponse setEmployeeEnabled(UserPrincipal actor, Long userId, boolean enabled) {
        AppUser user = getUser(userId);
        Guard.input(user.getRole() != Role.CUSTOMER, "Chỉ áp dụng cho tài khoản nhân viên");
        if (user.getId().equals(actor.getId()) && !enabled) {
            throw new BadRequestException("Không thể tự khóa tài khoản của chính mình");
        }
        user.setEnabled(enabled);
        return toEmployeeResponse(user, employees.findByUserId(userId).orElse(null));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(Long userId) {
        AppUser user = getUser(userId);
        Guard.input(user.getRole() != Role.CUSTOMER, "Không phải tài khoản nhân viên");
        return toEmployeeResponse(user, employees.findByUserId(userId).orElse(null));
    }

    // ------------------------------------------------------------- helpers
    private static void requireCustomer(UserPrincipal actor) {
        if (!actor.isCustomer()) {
            throw new ForbiddenException("Chức năng chỉ dành cho khách hàng");
        }
    }

    private EmployeeResponse toEmployeeResponse(AppUser u, EmployeeProfile p) {
        long active = u.getRole() == Role.DRIVER_ESCORT
                ? assignments.countByStaffIdAndTripStatusIn(u.getId(), ACTIVE_TRIP_STATUSES) : 0;
        return new EmployeeResponse(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(),
                Boolean.TRUE.equals(u.getEnabled()),
                p == null ? null : p.getEmployeeCode(), p == null ? null : p.getDepartment(),
                p == null ? null : p.getStaffRole(), p == null ? null : p.getLicenseNumber(),
                p == null ? null : p.getLicenseExpiry(), p != null && Boolean.TRUE.equals(p.getAirEscortCertified()),
                p == null ? null : p.getEmergencyContact(), p == null ? null : p.getBaseLocation(), active);
    }

    public static UserSummary toSummary(AppUser u) {
        return new UserSummary(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(),
                Boolean.TRUE.equals(u.getEnabled()), u.getCreatedAt(), u.getLastLoginAt());
    }

    private static CustomerProfileResponse toCustomerResponse(AppUser u, CustomerProfile p) {
        return new CustomerProfileResponse(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(),
                p.getCompanyName(), p.getCustomerType(), p.getAddress(), p.getCountry(), p.getTaxCode(),
                p.getPreferredLanguage(), u.getCreatedAt());
    }

    /** Dùng cho validate phân công. */
    public StaffRole staffRoleOf(Long userId) {
        return employees.findByUserId(userId).map(EmployeeProfile::getStaffRole).orElse(null);
    }

    /**
     * Xóa (lưu trữ) hồ sơ nhân sự đã nghỉ việc/bị sa thải: khóa đăng nhập và ẩn khỏi danh sách phân công.
     * Manager xóa được mọi nhân viên (trừ chính mình); Điều phối viên chỉ xóa được tài xế/áp tải.
     * Không xóa khi người đó còn trong chuyến chưa kết thúc – phải gỡ phân công trước. Lịch sử chuyến vẫn giữ nguyên.
     */
    @Transactional
    public void removeEmployee(UserPrincipal actor, Long userId) {
        AppUser user = getUser(userId);
        Guard.input(user.getRole() != Role.CUSTOMER, "Chỉ áp dụng cho tài khoản nhân viên");
        if (user.getId().equals(actor.getId())) {
            throw new BadRequestException("Không thể xóa chính tài khoản của mình");
        }
        if (!actor.isManager() && user.getRole() != Role.DRIVER_ESCORT) {
            throw new ForbiddenException("Điều phối viên chỉ được xóa tài xế / người áp tải");
        }
        EmployeeProfile p = employees.findByUserId(userId).orElse(null);
        Guard.state(!isArchived(p), "Nhân sự đã được xóa trước đó");
        Guard.state(assignments.countByStaffIdAndTripStatusIn(userId, ACTIVE_TRIP_STATUSES) == 0,
                "Nhân sự đang được phân công cho chuyến chưa kết thúc; hãy gỡ phân công hoặc chờ chuyến kết thúc");
        if (user.getRole() == Role.LOGISTICS_MANAGER) {
            long remaining = users.findByRoleAndEnabledTrue(Role.LOGISTICS_MANAGER).stream()
                    .filter(m -> !m.getId().equals(userId)).count();
            Guard.state(remaining > 0, "Không thể xóa Logistics Manager cuối cùng của hệ thống");
        }
        user.setEnabled(false);
        if (p != null) {
            p.setArchived(true);
            p.setArchivedAt(LocalDateTime.now());
        }
    }
}
