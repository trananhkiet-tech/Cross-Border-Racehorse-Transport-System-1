package com.example.racetransport.tripexpense.controller;

import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.tripexpense.dto.ExpenseDtos.ExpenseRequest;
import com.example.racetransport.tripexpense.dto.ExpenseDtos.ExpenseResponse;
import com.example.racetransport.tripexpense.dto.ExpenseDtos.ReviewRequest;
import com.example.racetransport.tripexpense.entity.ExpenseStatus;
import com.example.racetransport.tripexpense.service.TripExpenseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Trip expenses")
public class TripExpenseController {

    private final TripExpenseService service;

    public TripExpenseController(TripExpenseService service) {
        this.service = service;
    }

    @PostMapping("/trips/{id}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DRIVER_ESCORT')")
    public ExpenseResponse create(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                  @Valid @RequestBody ExpenseRequest req) {
        return service.create(actor, id, req);
    }

    @GetMapping("/expenses")
    @PreAuthorize("hasAnyRole('DRIVER_ESCORT','LOGISTICS_MANAGER','FLEET_ROUTE_COORDINATOR')")
    public List<ExpenseResponse> list(@AuthenticationPrincipal UserPrincipal actor,
                                      @RequestParam(required = false) ExpenseStatus status) {
        return service.list(actor, status);
    }

    @PostMapping("/expenses/{id}/review")
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','FLEET_ROUTE_COORDINATOR')")
    public ExpenseResponse review(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                  @Valid @RequestBody ReviewRequest req) {
        return service.review(actor, id, req);
    }
}
