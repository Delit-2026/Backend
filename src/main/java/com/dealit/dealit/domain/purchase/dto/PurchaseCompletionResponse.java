package com.dealit.dealit.domain.purchase.dto;

import com.dealit.dealit.domain.purchase.entity.PurchaseStatus;
import java.time.OffsetDateTime;

public record PurchaseCompletionResponse(
	Long purchaseId,
	PurchaseStatus status,
	boolean buyerCompleted,
	boolean sellerCompleted,
	OffsetDateTime buyerCompletedAt,
	OffsetDateTime sellerCompletedAt,
	OffsetDateTime completedAt,
	OffsetDateTime settledAt
) {
}
