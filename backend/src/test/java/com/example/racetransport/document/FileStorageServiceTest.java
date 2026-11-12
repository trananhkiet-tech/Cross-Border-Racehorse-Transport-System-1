package com.example.racetransport.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.racetransport.common.exception.BadRequestException;
import com.example.racetransport.document.entity.StoredFile;
import com.example.racetransport.document.repository.StoredFileRepository;
import com.example.racetransport.document.service.FileStorageService;
import com.example.racetransport.support.TestData;
import com.example.racetransport.user.entity.Role;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/** Upload an toàn: kiểm tra magic bytes, tên lưu UUID, không path traversal. */
class FileStorageServiceTest {

    private FileStorageService storage;
    private Path dir;

    @BeforeEach
    void setUp() throws Exception {
        dir = Files.createTempDirectory("irtms-upload-test");
        StoredFileRepository repo = mock(StoredFileRepository.class);
        when(repo.save(any(StoredFile.class))).thenAnswer(inv -> inv.getArgument(0));
        storage = new FileStorageService(repo, TestData.props(dir.toString()));
    }

    @Test
    void storesPdfWithRandomNameAndSafeDisplayName() throws Exception {
        MockMultipartFile f = new MockMultipartFile("file", "../../etc/passwd.pdf", "application/pdf",
                "%PDF-1.4 demo".getBytes(StandardCharsets.US_ASCII));
        StoredFile sf = storage.store(f, TestData.user(1, Role.CUSTOMER, "A"));
        assertEquals("application/pdf", sf.getContentType());
        assertEquals("passwd.pdf", sf.getOriginalFilename());
        assertFalse(sf.getStoredFilename().contains(".."));
        assertTrue(sf.getStoredFilename().endsWith(".pdf"));
        assertEquals(64, sf.getSha256().length());
        assertTrue(Files.exists(dir.resolve(sf.getStoredFilename())));
    }

    @Test
    void rejectsExecutableDisguisedAsPdf() {
        MockMultipartFile exe = new MockMultipartFile("file", "invoice.pdf", "application/pdf",
                new byte[] {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0});
        assertThrows(BadRequestException.class, () -> storage.store(exe, TestData.user(1, Role.CUSTOMER, "A")));
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);
        assertThrows(BadRequestException.class, () -> storage.store(empty, TestData.user(1, Role.CUSTOMER, "A")));
    }

    @Test
    void detectsPngByMagicBytesAndForcesExtension() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile f = new MockMultipartFile("file", "photo.exe", "application/octet-stream", png);
        StoredFile sf = storage.store(f, TestData.user(1, Role.DRIVER_ESCORT, "B"));
        assertEquals("image/png", sf.getContentType());
        assertEquals("photo.png", sf.getOriginalFilename());
    }
}
