package com.example.racetransport.horse.controller;

import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.horse.dto.HorseDtos.HorseRequest;
import com.example.racetransport.horse.dto.HorseDtos.HorseResponse;
import com.example.racetransport.horse.entity.HorseStatus;
import com.example.racetransport.horse.service.HorseService;
import com.example.racetransport.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Horse Profile. Tài liệu ngựa: /api/horses/{id}/documents; lịch sử sức khỏe: /api/horses/{id}/health-history. */
@RestController
@RequestMapping("/api/horses")
@Tag(name = "Horses")
public class HorseController {

    private final HorseService service;

    public HorseController(HorseService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','LOGISTICS_MANAGER','TRANSPORT_SPECIALIST','FLEET_ROUTE_COORDINATOR')")
    public PageResponse<HorseResponse> list(@AuthenticationPrincipal UserPrincipal actor,
                                            @RequestParam(required = false) Long ownerId,
                                            @RequestParam(required = false) HorseStatus status,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return service.list(actor, ownerId, status, q, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public HorseResponse create(@AuthenticationPrincipal UserPrincipal actor, @Valid @RequestBody HorseRequest req) {
        return service.create(actor, req);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','LOGISTICS_MANAGER','TRANSPORT_SPECIALIST','FLEET_ROUTE_COORDINATOR')")
    public HorseResponse get(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return service.get(actor, id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public HorseResponse update(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                @Valid @RequestBody HorseRequest req) {
        return service.update(actor, id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Lưu trữ hồ sơ ngựa (soft delete, giữ lịch sử)")
    public HorseResponse archive(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return service.archive(actor, id);
    }
}
