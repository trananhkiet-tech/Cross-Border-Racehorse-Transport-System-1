package com.example.racetransport.document.service;

import com.example.racetransport.common.exception.BadRequestException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.config.AppProperties;
import com.example.racetransport.document.entity.StoredFile;
import com.example.racetransport.document.repository.StoredFileRepository;
import com.example.racetransport.user.entity.AppUser;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lưu tệp tải lên vào thư mục cục bộ (UPLOAD_DIR) an toàn:
 * - giới hạn dung lượng; chỉ nhận PDF/JPEG/PNG/WEBP, xác định loại bằng magic bytes (không tin header/extension);
 * - tên lưu trữ = UUID + phần mở rộng chuẩn, không dùng tên người dùng → không path traversal, không file thực thi;
 * - lưu SHA-256 để đối chiếu toàn vẹn.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final StoredFileRepository repository;
    private final Path root;
    private final long maxSize;
    private final java.util.List<String> allowedTypes;

    public FileStorageService(StoredFileRepository repository, AppProperties props) {
        this.repository = repository;
        this.root = Path.of(props.storage().uploadDir()).toAbsolutePath().normalize();
        this.maxSize = props.storage().maxFileSizeBytes();
        this.allowedTypes = props.storage().allowedTypes();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Không tạo được thư mục upload: " + root, e);
        }
    }

    public StoredFile store(MultipartFile file, AppUser uploader) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Chưa chọn tệp hoặc tệp rỗng");
        }
        if (file.getSize() > maxSize) {
            throw new BadRequestException("Tệp vượt quá dung lượng cho phép (" + (maxSize / 1024 / 1024) + "MB)");
        }
        byte[] head = new byte[16];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            throw new BadRequestException("Không đọc được tệp tải lên");
        }
        String detected = detectType(head, read);
        if (detected == null || !allowedTypes.contains(detected)) {
            throw new BadRequestException("Chỉ chấp nhận tệp PDF, JPG, PNG hoặc WEBP");
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + extensionOf(detected);
        LocalDate today = LocalDate.now();
        Path dir = root.resolve(String.valueOf(today.getYear())).resolve(String.format("%02d", today.getMonthValue()));
        Path target = dir.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new BadRequestException("Đường dẫn lưu tệp không hợp lệ");
        }
        String sha256;
        try {
            Files.createDirectories(dir);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new java.security.DigestInputStream(file.getInputStream(), md)) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            sha256 = HexFormat.of().formatHex(md.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("Lưu tệp thất bại", e);
            throw new BadRequestException("Không lưu được tệp, vui lòng thử lại");
        }
        StoredFile sf = new StoredFile();
        sf.setOriginalFilename(sanitizeName(file.getOriginalFilename(), extensionOf(detected)));
        sf.setStoredFilename(today.getYear() + "/" + String.format("%02d", today.getMonthValue()) + "/" + storedName);
        sf.setContentType(detected);
        sf.setSizeBytes(file.getSize());
        sf.setSha256(sha256);
        sf.setUploadedBy(uploader);
        return repository.save(sf);
    }

    /** Mở tệp để tải xuống; luôn kiểm tra lại đường dẫn nằm trong thư mục gốc. */
    public Resource open(StoredFile sf) {
        Path p = root.resolve(sf.getStoredFilename()).normalize();
        if (!p.startsWith(root) || !Files.isRegularFile(p)) {
            throw new NotFoundException("Tệp không còn tồn tại trên máy chủ");
        }
        return new FileSystemResource(p);
    }

    /** Nhận dạng loại tệp từ magic bytes. */
    static String detectType(byte[] b, int n) {
        if (n >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') {
            return "application/pdf";
        }
        if (n >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (n >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        if (n >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private static String extensionOf(String type) {
        return switch (type) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".bin";
        };
    }

    /** Chỉ giữ tên hiển thị an toàn (bỏ thư mục, ký tự điều khiển), ép đúng phần mở rộng thật. */
    static String sanitizeName(String original, String ext) {
        String name = original == null ? "file" : original;
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[\\p{Cntrl}<>:\"|?*]", "_").trim();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        if (base.isBlank() || base.equals(".") || base.equals("..")) {
            base = "file";
        }
        if (base.length() > 200) {
            base = base.substring(0, 200);
        }
        return base + ext;
    }
}
