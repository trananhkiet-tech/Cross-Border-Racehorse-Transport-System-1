package com.example.racetransport.user.controller;

import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.user.dto.UserDtos.CustomerProfileResponse;
import com.example.racetransport.user.dto.UserDtos.UpdateCustomerProfileRequest;
import com.example.racetransport.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

    private final UserService userService;

    public CustomerController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public CustomerProfileResponse me(@AuthenticationPrincipal UserPrincipal actor) {
        return userService.getCustomerProfile(actor);
    }

    @PutMapping("/me")
    public CustomerProfileResponse update(@AuthenticationPrincipal UserPrincipal actor,
                                          @Valid @RequestBody UpdateCustomerProfileRequest req) {
        return userService.updateCustomerProfile(actor, req);
    }
}
