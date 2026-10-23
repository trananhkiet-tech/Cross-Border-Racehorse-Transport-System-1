package com.example.racetransport.document.controller;

import com.example.racetransport.document.entity.StoredFile;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Tạo response tải tệp an toàn (Content-Disposition chuẩn, nosniff). */
public final class FileResponses {

    private FileResponses() { }

    public static ResponseEntity<Resource> of(StoredFile f, Resource resource, boolean inline) {
        ContentDisposition cd = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(f.getOriginalFilename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(f.getContentType()))
                .contentLength(f.getSizeBytes())
                .body(resource);
    }
}
