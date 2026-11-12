package com.example.racetransport.document.controller;

import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.document.dto.DocumentDtos.DocumentResponse;
import com.example.racetransport.document.dto.DocumentDtos.ReviewRequest;
import com.example.racetransport.document.entity.DocumentReviewStatus;
import com.example.racetransport.document.entity.DocumentType;
import com.example.racetransport.document.entity.StoredFile;
import com.example.racetransport.document.service.DocumentService;
import com.example.racetransport.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
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
@Tag(name = "Documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping(value = "/api/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER','TRANSPORT_SPECIALIST')")
    @Operation(summary = "Tải tài liệu (PDF/JPG/PNG/WEBP ≤ 10MB) cho yêu cầu và/hoặc ngựa")
    public DocumentResponse upload(@AuthenticationPrincipal UserPrincipal actor,
                                   @RequestPart("file") MultipartFile file,
                                   @RequestParam DocumentType documentType,
                                   @RequestParam(required = false) Long requestId,
                                   @RequestParam(required = false) Long horseId,
                                   @RequestParam(required = false) Long checklistItemId,
                                   @RequestParam(required = false) String title,
                                   @RequestParam(required = false) String documentNumber,
                                   @RequestParam(required = false) String issuingCountry,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate) {
        return service.upload(actor, file, documentType, requestId, horseId, checklistItemId, title, documentNumber,
                issuingCountry, issueDate, expiryDate);
    }

    @GetMapping("/api/documents")
    @PreAuthorize("hasAnyRole('CUSTOMER','LOGISTICS_MANAGER','TRANSPORT_SPECIALIST','FLEET_ROUTE_COORDINATOR')")
    public PageResponse<DocumentResponse> search(@AuthenticationPrincipal UserPrincipal actor,
                                                 @RequestParam(required = false) List<DocumentReviewStatus> status,
                                                 @RequestParam(required = false) List<DocumentType> type,
                                                 @RequestParam(required = false) String q,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return service.search(actor, status, type, q, page, size);
    }

    @GetMapping("/api/documents/expiring")
    @PreAuthorize("hasAnyRole('LOGISTICS_MANAGER','TRANSPORT_SPECIALIST')")
    public List<DocumentResponse> expiring(@RequestParam(defaultValue = "30") int days) {
        return service.expiring(days);
    }

    @PostMapping("/api/documents/mark-expired")
    @PreAuthorize("hasRole('TRANSPORT_SPECIALIST')")
    public Map<String, Integer> markExpired(@AuthenticationPrincipal UserPrincipal actor) {
        return Map.of("updated", service.markExpired(actor));
    }

    @GetMapping("/api/documents/{id}")
    public DocumentResponse get(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return service.get(actor, id);
    }

    /** Tải/ xem tệp – kiểm tra quyền như xem tài liệu. */
    @GetMapping("/api/documents/{id}/file")
    public ResponseEntity<org.springframework.core.io.Resource> download(@AuthenticationPrincipal UserPrincipal actor,
                                                                        @PathVariable Long id,
                                                                        @RequestParam(defaultValue = "false") boolean inline) {
        StoredFile f = service.fileOf(actor, id);
        return FileResponses.of(f, service.open(f), inline);
    }

    @GetMapping("/api/transportation-requests/{requestId}/documents")
    public List<DocumentResponse> forRequest(@AuthenticationPrincipal UserPrincipal actor,
                                             @PathVariable Long requestId) {
        return service.listForRequest(actor, requestId);
    }

    @GetMapping("/api/horses/{horseId}/documents")
    public List<DocumentResponse> forHorse(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long horseId) {
        return service.listForHorse(actor, horseId);
    }

    @PostMapping("/api/documents/{id}/review")
    @PreAuthorize("hasRole('TRANSPORT_SPECIALIST')")
    public DocumentResponse review(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                   @Valid @RequestBody ReviewRequest req) {
        return service.review(actor, id, req);
    }
}
