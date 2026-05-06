package com.dealit.dealit.domain.purchase.dto;

import com.dealit.dealit.domain.purchase.entity.PurchaseStatus;
import java.time.OffsetDateTime;

public record PurchaseReceiptResponse(
	Long purchaseId,
	Long productId,
	String productTitle,
	String productImageUrl,
	Long buyerId,
	Long sellerId,
	long amount,
	PurchaseStatus status,
	OffsetDateTime purchasedAt,
	Long chatRoomId
) {
}
