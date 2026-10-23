package com.example.racetransport.notification.service;

import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.response.PageResponse;
import com.example.racetransport.notification.dto.NotificationResponse;
import com.example.racetransport.notification.entity.Notification;
import com.example.racetransport.notification.entity.NotificationType;
import com.example.racetransport.notification.repository.NotificationRepository;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thông báo trong ứng dụng (lưu DB). Các service nghiệp vụ gọi notify(...) trong cùng
 * transaction để thông báo nhất quán với thay đổi dữ liệu.
 */
@Service
public class NotificationService {

    /** Loại tham chiếu để frontend điều hướng tới đúng trang. */
    public static final String REF_REQUEST = "TRANSPORTATION_REQUEST";
    public static final String REF_DOCUMENT = "DOCUMENT";
    public static final String REF_QUOTE = "QUOTE";
    public static final String REF_INVOICE = "INVOICE";
    public static final String REF_TRIP = "TRIP";
    public static final String REF_INCIDENT = "INCIDENT";
    public static final String REF_HANDOVER = "HANDOVER";
    public static final String REF_CLAIM = "CLAIM";

    private final NotificationRepository repository;
    private final AppUserRepository users;
    private final Clock clock;

    public NotificationService(NotificationRepository repository, AppUserRepository users, Clock clock) {
        this.repository = repository;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public void notify(AppUser recipient, NotificationType type, String title, String message,
                       String referenceType, Long referenceId) {
        if (recipient == null) {
            return;
        }
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setType(type);
        n.setTitle(truncate(title, 200));
        n.setMessage(truncate(message, 1000));
        n.setReferenceType(referenceType);
        n.setReferenceId(referenceId);
        repository.save(n);
    }

    /** Gửi cho tất cả người dùng đang hoạt động của một role. */
    @Transactional
    public void notifyRole(Role role, NotificationType type, String title, String message,
                           String referenceType, Long referenceId) {
        users.findByRoleAndEnabledTrue(role)
                .forEach(u -> notify(u, type, title, message, referenceType, referenceId));
    }

    /** Gửi cho nhiều người, bỏ trùng. */
    @Transactional
    public void notifyAll(Collection<AppUser> recipients, NotificationType type, String title, String message,
                          String referenceType, Long referenceId) {
        Set<Long> seen = new LinkedHashSet<>();
        recipients.stream().filter(Objects::nonNull).filter(u -> seen.add(u.getId()))
                .forEach(u -> notify(u, type, title, message, referenceType, referenceId));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UserPrincipal actor, boolean unreadOnly, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        var result = unreadOnly
                ? repository.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(actor.getId(), pr)
                : repository.findByRecipientIdOrderByCreatedAtDesc(actor.getId(), pr);
        return PageResponse.of(result, NotificationService::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UserPrincipal actor) {
        return repository.countByRecipientIdAndReadAtIsNull(actor.getId());
    }

    @Transactional
    public NotificationResponse markRead(UserPrincipal actor, Long id) {
        Notification n = repository.findById(id)
                .filter(x -> x.getRecipient().getId().equals(actor.getId()))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông báo"));
        if (n.getReadAt() == null) {
            n.setReadAt(LocalDateTime.now(clock));
        }
        return toResponse(n);
    }

    @Transactional
    public int markAllRead(UserPrincipal actor) {
        return repository.markAllRead(actor.getId(), LocalDateTime.now(clock));
    }

    public static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getTitle(), n.getMessage(), n.getType(),
                n.getReferenceType(), n.getReferenceId(), n.getReadAt() != null, n.getReadAt(), n.getCreatedAt());
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
