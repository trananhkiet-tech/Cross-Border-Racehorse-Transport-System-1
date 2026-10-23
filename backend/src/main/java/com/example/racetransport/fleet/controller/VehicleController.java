package com.example.racetransport.fleet.controller;

import com.example.racetransport.fleet.dto.VehicleDtos.VehicleRequest;
import com.example.racetransport.fleet.dto.VehicleDtos.VehicleResponse;
import com.example.racetransport.fleet.dto.VehicleDtos.VehicleStatusRequest;
import com.example.racetransport.fleet.entity.VehicleStatus;
import com.example.racetransport.fleet.entity.VehicleType;
import com.example.racetransport.fleet.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicles")
@Tag(name = "Fleet")
public class VehicleController {

    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FLEET_ROUTE_COORDINATOR','LOGISTICS_MANAGER')")
    public List<VehicleResponse> list(@RequestParam(required = false) List<VehicleStatus> status,
                                      @RequestParam(required = false) List<VehicleType> type,
                                      @RequestParam(required = false) String q) {
        return service.list(status, type, q);
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('FLEET_ROUTE_COORDINATOR','LOGISTICS_MANAGER')")
    @Operation(summary = "Xe sẵn sàng, không trùng lịch, đủ sức chứa")
    public List<VehicleResponse> available(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "1") int minCapacity,
            @RequestParam(required = false) Long excludeTripId) {
        return service.available(from, to, minCapacity, excludeTripId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FLEET_ROUTE_COORDINATOR','LOGISTICS_MANAGER')")
    public VehicleResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public VehicleResponse create(@Valid @RequestBody VehicleRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public VehicleResponse update(@PathVariable Long id, @Valid @RequestBody VehicleRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public VehicleResponse status(@PathVariable Long id, @Valid @RequestBody VehicleStatusRequest req) {
        return service.changeStatus(id, req.status(), req.note());
    }

    /** Lưu trữ (soft delete) – giữ lịch sử chuyến. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public VehicleResponse archive(@PathVariable Long id) {
        return service.archive(id);
    }
}
