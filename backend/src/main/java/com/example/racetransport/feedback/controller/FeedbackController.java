package com.example.racetransport.feedback.controller;

import com.example.racetransport.feedback.dto.FeedbackDtos.DeliveredTripItem;
import com.example.racetransport.feedback.dto.FeedbackDtos.FeedbackRequest;
import com.example.racetransport.feedback.dto.FeedbackDtos.FeedbackResponse;
import com.example.racetransport.feedback.dto.FeedbackDtos.FeedbackSummary;
import com.example.racetransport.feedback.service.FeedbackService;
import com.example.racetransport.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Trip feedback")
public class FeedbackController {

    private final FeedbackService service;

    public FeedbackController(FeedbackService service) {
        this.service = service;
    }

    @PostMapping("/trips/{id}/feedback")
    @PreAuthorize("hasRole('CUSTOMER')")
    public FeedbackResponse submit(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                   @Valid @RequestBody FeedbackRequest req) {
        return service.submit(actor, id, req);
    }

    @GetMapping("/trips/{id}/feedback")
    @PreAuthorize("hasAnyRole('CUSTOMER','LOGISTICS_MANAGER')")
    public FeedbackResponse get(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return service.get(actor, id);
    }

    @GetMapping("/feedback/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<DeliveredTripItem> mine(@AuthenticationPrincipal UserPrincipal actor) {
        return service.mine(actor);
    }

    @GetMapping("/reports/feedback-summary")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    public FeedbackSummary summary() {
        return service.summary();
    }
}
