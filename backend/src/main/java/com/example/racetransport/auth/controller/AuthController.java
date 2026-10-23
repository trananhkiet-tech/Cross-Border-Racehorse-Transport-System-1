package com.example.racetransport.auth.controller;

import com.example.racetransport.auth.dto.AuthDtos.AuthResponse;
import com.example.racetransport.auth.dto.AuthDtos.LoginRequest;
import com.example.racetransport.auth.dto.AuthDtos.RegisterRequest;
import com.example.racetransport.auth.service.AuthService;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.user.dto.UserDtos.UserSummary;
import com.example.racetransport.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /** Public: tạo tài khoản CUSTOMER. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    /** Public: đăng nhập, trả JWT. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    /** Mọi role đã đăng nhập. */
    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal UserPrincipal actor) {
        return userService.me(actor);
    }
}
