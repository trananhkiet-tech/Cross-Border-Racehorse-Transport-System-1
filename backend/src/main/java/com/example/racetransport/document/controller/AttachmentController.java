package com.example.racetransport.document.controller;

import com.example.racetransport.document.dto.DocumentDtos.AttachmentResponse;
import com.example.racetransport.document.entity.AttachmentRefType;
import com.example.racetransport.document.entity.StoredFile;
import com.example.racetransport.document.service.AttachmentService;
import com.example.racetransport.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Ảnh/tệp đính kèm cho đối tượng trong chuyến (quan sát, sự cố, bàn giao...). */
@RestController
@RequestMapping("/api/attachments")
@Tag(name = "Attachments")
public class AttachmentController {

    private final AttachmentService service;

    public AttachmentController(AttachmentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse upload(@AuthenticationPrincipal UserPrincipal actor,
                                     @RequestPart("file") MultipartFile file,
                                     @RequestParam AttachmentRefType refType,
                                     @RequestParam Long refId,
                                     @RequestParam(required = false) String caption) {
        return service.upload(actor, refType, refId, caption, file);
    }

    @GetMapping
    public List<AttachmentResponse> list(@AuthenticationPrincipal UserPrincipal actor,
                                         @RequestParam AttachmentRefType refType, @RequestParam Long refId) {
        return service.list(actor, refType, refId);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                         @RequestParam(defaultValue = "true") boolean inline) {
        StoredFile f = service.fileOf(actor, id);
        return FileResponses.of(f, service.open(f), inline);
    }
}
