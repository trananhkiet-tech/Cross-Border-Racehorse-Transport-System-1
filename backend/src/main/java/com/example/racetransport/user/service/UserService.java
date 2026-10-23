package com.example.racetransport.user.service;

import com.example.racetransport.common.exception.BadRequestException;
import com.example.racetransport.common.exception.ConflictException;
import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.util.Countries;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.security.UserPrincipal;
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

    private final AppUserRepository users;
    private final CustomerProfileRepository customers;
    private final EmployeeProfileRepository employees;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository users, CustomerProfileRepository customers,
                       EmployeeProfileRepository employees,
                       PasswordEncoder passwordEncoder) {
        this.users = users;
        this.customers = customers;
        this.employees = employees;
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
        return list.stream().map(u -> toEmployeeResponse(u, profiles.get(u.getId()))).toList();
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
        // Số chuyến đang phụ trách do module Trip (user3) bổ sung khi merge; bản main chưa phụ thuộc module Trip.
        long active = 0;
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
}
