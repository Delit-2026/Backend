package com.dealit.dealit.domain.purchase.dto;

import com.dealit.dealit.domain.purchase.entity.PurchaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "일반 상품 구매 응답")
public record PurchaseResponse(
	@Schema(description = "구매 ID", example = "1")
	Long purchaseId,

	@Schema(description = "상품 ID", example = "10")
	Long productId,

	@Schema(description = "결제 금액", example = "30000")
	long amount,

	@Schema(description = "구매 상태", example = "PAID", allowableValues = {"PAID", "COMPLETED"})
	PurchaseStatus status,

	@Schema(description = "구매 일시", example = "2026-05-06T21:30:00+09:00")
	OffsetDateTime purchasedAt
) {
}
