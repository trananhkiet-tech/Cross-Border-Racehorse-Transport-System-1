package com.example.racetransport.notification.dto;

import com.example.racetransport.notification.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(Long id, String title, String message, NotificationType type,
                                   String referenceType, Long referenceId, boolean read,
                                   LocalDateTime readAt, LocalDateTime createdAt) { }
