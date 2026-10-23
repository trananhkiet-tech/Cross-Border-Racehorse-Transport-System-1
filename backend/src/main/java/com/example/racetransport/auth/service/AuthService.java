package com.example.racetransport.auth.service;

import com.example.racetransport.auth.dto.AuthDtos.AuthResponse;
import com.example.racetransport.auth.dto.AuthDtos.LoginRequest;
import com.example.racetransport.auth.dto.AuthDtos.RegisterRequest;
import com.example.racetransport.common.exception.ConflictException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.security.JwtService;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.CustomerProfile;
import com.example.racetransport.user.entity.CustomerType;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import com.example.racetransport.user.repository.CustomerProfileRepository;
import com.example.racetransport.user.service.UserService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đăng ký (chỉ CUSTOMER), đăng nhập và lấy thông tin người dùng hiện tại. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AppUserRepository users;
    private final CustomerProfileRepository customers;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final Clock clock;

    public AuthService(AppUserRepository users, CustomerProfileRepository customers, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService, Clock clock) {
        this.users = users;
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalizeEmail(req.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email đã được sử dụng");
        }
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone());
        user.setRole(Role.CUSTOMER); // luôn là CUSTOMER
        user.setEnabled(true);
        user.setLastLoginAt(LocalDateTime.now(clock));
        users.save(user);

        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setCompanyName(req.companyName());
        profile.setCustomerType(req.customerType() == null ? CustomerType.INDIVIDUAL : req.customerType());
        profile.setCountry(req.country());
        customers.save(profile);

        log.info("Registered new customer id={}", user.getId());
        UserPrincipal principal = UserPrincipal.from(user);
        return new AuthResponse(jwtService.generateToken(principal), "Bearer", jwtService.getExpirationSeconds(),
                UserService.toSummary(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizeEmail(req.email()), req.password()));
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        AppUser user = users.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản"));
        user.setLastLoginAt(LocalDateTime.now(clock));
        return new AuthResponse(jwtService.generateToken(principal), "Bearer", jwtService.getExpirationSeconds(),
                UserService.toSummary(user));
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
