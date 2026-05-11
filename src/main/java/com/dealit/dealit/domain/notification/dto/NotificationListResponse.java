package com.dealit.dealit.domain.notification.dto;

import java.util.List;

public record NotificationListResponse(
	List<NotificationResponse> content,
	int page,
	int size,
	long totalElements,
	boolean hasNext
) {
}
