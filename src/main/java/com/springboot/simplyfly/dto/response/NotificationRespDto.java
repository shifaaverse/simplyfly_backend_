package com.springboot.simplyfly.dto.response;

import java.time.Instant;

public record NotificationRespDto(
        Long notificationId,
        String title,
        String message,
        boolean isRead,
        Instant createdAt
) {
}
