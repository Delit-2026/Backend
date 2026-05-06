package com.dealit.dealit.domain.notification.dto;

import com.dealit.dealit.domain.notification.entity.InAppNotificationType;
import java.time.OffsetDateTime;

public record NotificationResponse(
	Long notificationId,
	InAppNotificationType type,
	String title,
	String content,
	boolean read,
	String targetType,
	Long targetId,
	String targetUrl,
	OffsetDateTime createdAt,
	OffsetDateTime readAt
) {
}
