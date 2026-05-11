package com.dealit.dealit.domain.notification.dto;

import com.dealit.dealit.domain.notification.entity.InAppNotificationType;

public record UnreadNotificationTypeCountResponse(
	InAppNotificationType type,
	long count
) {
}
