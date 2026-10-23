package com.example.racetransport.auth.dto;

import com.example.racetransport.user.dto.UserDtos.UserSummary;
import com.example.racetransport.user.entity.CustomerType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() { }

    /** Đăng ký công khai – luôn tạo tài khoản CUSTOMER, không nhận role từ client. */
    public record RegisterRequest(
            @NotBlank @Email(message = "Email không hợp lệ") @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72, message = "Mật khẩu từ 8 đến 72 ký tự")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Mật khẩu phải có cả chữ và số")
            String password,
            @NotBlank @Size(max = 150) String fullName,
            @Size(max = 30) @Pattern(regexp = "^[0-9+()\\-\\s]*$", message = "Số điện thoại không hợp lệ") String phone,
            @Size(max = 150) String companyName,
            CustomerType customerType,
            @Size(min = 2, max = 2) String country,
            @AssertTrue(message = "Bạn cần đồng ý điều khoản sử dụng") Boolean acceptTerms) { }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) { }

    public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserSummary user) { }
}
