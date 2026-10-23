package com.example.racetransport.user.dto;

import com.example.racetransport.user.entity.CustomerType;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.entity.StaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** DTO của module user (gộp trong một file để dễ tra cứu). */
public final class UserDtos {

    private UserDtos() { }

    /** Thông tin tóm tắt người dùng hiện tại. */
    public record UserSummary(Long id, String email, String fullName, String phone, Role role, boolean enabled,
                              LocalDateTime createdAt, LocalDateTime lastLoginAt) { }

    public record CustomerProfileResponse(Long userId, String email, String fullName, String phone,
                                          String companyName, CustomerType customerType, String address,
                                          String country, String taxCode, String preferredLanguage,
                                          LocalDateTime createdAt) { }

    public record UpdateCustomerProfileRequest(
            @NotBlank @Size(max = 150) String fullName,
            @Size(max = 30) @Pattern(regexp = "^[0-9+()\\-\\s]*$", message = "Số điện thoại không hợp lệ") String phone,
            @Size(max = 150) String companyName,
            @NotNull CustomerType customerType,
            @Size(max = 255) String address,
            @Size(min = 2, max = 2) String country,
            @Size(max = 30) String taxCode,
            @Pattern(regexp = "vi|en") String preferredLanguage) { }

    public record UpdateMyAccountRequest(
            @NotBlank @Size(max = 150) String fullName,
            @Size(max = 30) @Pattern(regexp = "^[0-9+()\\-\\s]*$", message = "Số điện thoại không hợp lệ") String phone) { }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72)
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Mật khẩu phải có cả chữ và số")
            String newPassword) { }

    public record EmployeeResponse(Long userId, String email, String fullName, String phone, Role role,
                                   boolean enabled, String employeeCode, String department, StaffRole staffRole,
                                   String licenseNumber, LocalDate licenseExpiry, boolean airEscortCertified,
                                   String emergencyContact, String baseLocation,
                                   long activeAssignments) { }

    /** Tạo tài khoản nhân viên – chỉ Logistics Manager (không qua đăng ký công khai). */
    public record CreateEmployeeRequest(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 150) String fullName,
            @Size(max = 30) String phone,
            @NotNull Role role,
            @NotBlank @Size(min = 8, max = 72) String initialPassword,
            @NotBlank @Size(max = 20) String employeeCode,
            @Size(max = 100) String department,
            StaffRole staffRole,
            @Size(max = 50) String licenseNumber,
            LocalDate licenseExpiry,
            Boolean airEscortCertified,
            @Size(max = 150) String emergencyContact,
            @Size(max = 150) String baseLocation) { }

    public record UpdateEmployeeStatusRequest(@NotNull Boolean enabled) { }
}
