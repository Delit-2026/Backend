package com.dealit.dealit.domain.purchase.dto;

import com.dealit.dealit.domain.purchase.entity.PurchaseStatus;
import java.time.OffsetDateTime;

public record PurchaseResponse(
	Long purchaseId,
	Long productId,
	long amount,
	PurchaseStatus status,
	OffsetDateTime purchasedAt
) {
}
