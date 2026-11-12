package com.example.racetransport.report.controller;

import com.example.racetransport.report.dto.ReportDtos.ClaimReport;
import com.example.racetransport.report.dto.ReportDtos.DashboardResponse;
import com.example.racetransport.report.dto.ReportDtos.IncidentReport;
import com.example.racetransport.report.dto.ReportDtos.KpiReport;
import com.example.racetransport.report.dto.ReportDtos.RevenueReport;
import com.example.racetransport.report.dto.ReportDtos.TransportReport;
import com.example.racetransport.report.service.DashboardService;
import com.example.racetransport.report.service.ReportService;
import com.example.racetransport.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Dashboard & Reports")
public class ReportController {

    private final ReportService reports;
    private final DashboardService dashboards;

    public ReportController(ReportService reports, DashboardService dashboards) {
        this.reports = reports;
        this.dashboards = dashboards;
    }

    @GetMapping("/api/dashboard")
    @Operation(summary = "Dashboard theo role của người dùng hiện tại")
    public DashboardResponse dashboard(@AuthenticationPrincipal UserPrincipal actor) {
        return dashboards.forUser(actor);
    }

    @GetMapping("/api/reports/revenue")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    public RevenueReport revenue() {
        return reports.revenue();
    }

    @GetMapping("/api/reports/transport")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    public TransportReport transport() {
        return reports.transport();
    }

    @GetMapping("/api/reports/incidents")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    public IncidentReport incidents() {
        return reports.incidents();
    }

    @GetMapping("/api/reports/claims")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    public ClaimReport claims() {
        return reports.claims();
    }

    @GetMapping("/api/reports/kpi")
    @PreAuthorize("hasRole('LOGISTICS_MANAGER')")
    public KpiReport kpi() {
        return reports.kpi();
    }
}
