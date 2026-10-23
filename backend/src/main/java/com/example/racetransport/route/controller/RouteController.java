package com.example.racetransport.route.controller;

import com.example.racetransport.route.dto.RouteDtos.RouteDetail;
import com.example.racetransport.route.dto.RouteDtos.RouteRequest;
import com.example.racetransport.route.dto.RouteDtos.RouteStopRequest;
import com.example.racetransport.route.dto.RouteDtos.RouteSummary;
import com.example.racetransport.route.entity.RouteStatus;
import com.example.racetransport.route.service.RouteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

@RestController
@RequestMapping("/api/routes")
@Tag(name = "Routes")
public class RouteController {

    private final RouteService service;

    public RouteController(RouteService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FLEET_ROUTE_COORDINATOR','LOGISTICS_MANAGER')")
    public List<RouteSummary> list(@RequestParam(required = false) List<RouteStatus> status,
                                   @RequestParam(required = false) String origin,
                                   @RequestParam(required = false) String destination,
                                   @RequestParam(required = false) String q) {
        return service.list(status, origin, destination, q);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FLEET_ROUTE_COORDINATOR','LOGISTICS_MANAGER')")
    public RouteDetail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public RouteDetail create(@Valid @RequestBody RouteRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public RouteDetail update(@PathVariable Long id, @Valid @RequestBody RouteRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/{id}/stops")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public RouteDetail addStop(@PathVariable Long id, @Valid @RequestBody RouteStopRequest req) {
        return service.addStop(id, req);
    }

    @PutMapping("/{routeId}/stops/{stopId}")
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public RouteDetail updateStop(@PathVariable Long routeId, @PathVariable Long stopId,
                                  @Valid @RequestBody RouteStopRequest req) {
        return service.updateStop(routeId, stopId, req);
    }

    @DeleteMapping("/{routeId}/stops/{stopId}")
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public RouteDetail deleteStop(@PathVariable Long routeId, @PathVariable Long stopId) {
        return service.deleteStop(routeId, stopId);
    }
}
