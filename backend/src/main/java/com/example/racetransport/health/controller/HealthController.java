package com.example.racetransport.health.controller;

import com.example.racetransport.document.controller.FileResponses;
import com.example.racetransport.document.entity.StoredFile;
import com.example.racetransport.health.dto.HealthDtos.ArrivalCheckRequest;
import com.example.racetransport.health.dto.HealthDtos.ArrivalCheckResponse;
import com.example.racetransport.health.dto.HealthDtos.HorseHealthHistory;
import com.example.racetransport.health.dto.HealthDtos.ObservationRequest;
import com.example.racetransport.health.dto.HealthDtos.ObservationResponse;
import com.example.racetransport.health.dto.HealthDtos.PreTransportCheckRequest;
import com.example.racetransport.health.dto.HealthDtos.PreTransportCheckResponse;
import com.example.racetransport.health.dto.HealthDtos.VetConsultationRequest;
import com.example.racetransport.health.dto.HealthDtos.VetConsultationResponse;
import com.example.racetransport.health.service.HealthService;
import com.example.racetransport.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "Health (observation, checks, external vet consultation)")
public class HealthController {

    private final HealthService service;

    public HealthController(HealthService service) {
        this.service = service;
    }

    @PostMapping("/api/health-checks/pre-transport")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DRIVER_ESCORT')")
    @Operation(summary = "Kiểm tra (quan sát) trước vận chuyển cho từng ngựa")
    public PreTransportCheckResponse preTransport(@AuthenticationPrincipal UserPrincipal actor,
                                                  @Valid @RequestBody PreTransportCheckRequest req) {
        return service.recordPreTransport(actor, req);
    }

    @GetMapping("/api/trips/{tripId}/pre-transport-checks")
    public List<PreTransportCheckResponse> preChecks(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long tripId) {
        return service.preChecksOf(actor, tripId);
    }

    @PostMapping("/api/health-observations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DRIVER_ESCORT')")
    @Operation(summary = "Ghi quan sát sức khỏe (không chẩn đoán); reportIncident=true để báo sự cố ngay")
    public ObservationResponse observe(@AuthenticationPrincipal UserPrincipal actor, @Valid @RequestBody ObservationRequest req) {
        return service.recordObservation(actor, req);
    }

    @GetMapping("/api/trips/{tripId}/health-observations")
    public List<ObservationResponse> observations(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long tripId) {
        return service.observationsOf(actor, tripId);
    }

    @PostMapping("/api/health-checks/arrival")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DRIVER_ESCORT')")
    public ArrivalCheckResponse arrival(@AuthenticationPrincipal UserPrincipal actor, @Valid @RequestBody ArrivalCheckRequest req) {
        return service.recordArrival(actor, req);
    }

    @GetMapping("/api/trips/{tripId}/arrival-checks")
    public List<ArrivalCheckResponse> arrivalChecks(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long tripId) {
        return service.arrivalChecksOf(actor, tripId);
    }

    @PostMapping("/api/veterinary-consultations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','TRANSPORT_SPECIALIST','DRIVER_ESCORT')")
    @Operation(summary = "Ghi nhận tư vấn từ bác sĩ thú y BÊN NGOÀI (không phải tài khoản hệ thống)")
    public VetConsultationResponse consult(@AuthenticationPrincipal UserPrincipal actor,
                                           @Valid @RequestBody VetConsultationRequest req) {
        return service.recordConsultation(actor, req);
    }

    @GetMapping("/api/veterinary-consultations/{id}")
    public VetConsultationResponse consultation(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return service.getConsultation(actor, id);
    }

    @PostMapping(value = "/api/veterinary-consultations/{id}/report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','TRANSPORT_SPECIALIST','DRIVER_ESCORT')")
    public VetConsultationResponse uploadReport(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                                @RequestPart("file") MultipartFile file) {
        return service.attachReport(actor, id, file);
    }

    @GetMapping("/api/veterinary-consultations/{id}/report")
    public ResponseEntity<Resource> report(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                           @RequestParam(defaultValue = "true") boolean inline) {
        StoredFile f = service.reportFile(actor, id);
        return FileResponses.of(f, service.open(f), inline);
    }

    @GetMapping("/api/horses/{horseId}/health-history")
    public HorseHealthHistory horseHistory(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long horseId) {
        return service.horseHistory(actor, horseId);
    }
}
