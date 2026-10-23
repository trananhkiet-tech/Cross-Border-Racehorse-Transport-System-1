package com.example.racetransport.user.controller;

import com.example.racetransport.common.response.MessageResponse;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.user.dto.UserDtos.ChangePasswordRequest;
import com.example.racetransport.user.dto.UserDtos.UpdateMyAccountRequest;
import com.example.racetransport.user.dto.UserDtos.UserSummary;
import com.example.racetransport.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tài khoản cá nhân – mọi role. */
@RestController
@RequestMapping("/api/users/me")
@Tag(name = "Account")
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping
    public UserSummary update(@AuthenticationPrincipal UserPrincipal actor,
                              @Valid @RequestBody UpdateMyAccountRequest req) {
        return userService.updateMyAccount(actor, req);
    }

    @PostMapping("/password")
    public MessageResponse changePassword(@AuthenticationPrincipal UserPrincipal actor,
                                          @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(actor, req);
        return new MessageResponse("Đã đổi mật khẩu");
    }
}
