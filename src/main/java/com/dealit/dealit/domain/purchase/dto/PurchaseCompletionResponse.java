package com.dealit.dealit.domain.purchase.dto;

import com.dealit.dealit.domain.purchase.entity.PurchaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "구매자/판매자 거래 완료 처리 응답")
public record PurchaseCompletionResponse(
	@Schema(description = "구매 ID", example = "1")
	Long purchaseId,

	@Schema(description = "구매 상태", example = "COMPLETED", allowableValues = {"PAID", "COMPLETED"})
	PurchaseStatus status,

	@Schema(description = "구매자 완료 여부", example = "true")
	boolean buyerCompleted,

	@Schema(description = "판매자 완료 여부", example = "true")
	boolean sellerCompleted,

	@Schema(description = "구매자 완료 처리 일시", example = "2026-05-06T22:00:00+09:00", nullable = true)
	OffsetDateTime buyerCompletedAt,

	@Schema(description = "판매자 완료 처리 일시", example = "2026-05-06T22:05:00+09:00", nullable = true)
	OffsetDateTime sellerCompletedAt,

	@Schema(description = "양측 완료로 거래가 최종 완료된 일시", example = "2026-05-06T22:05:00+09:00", nullable = true)
	OffsetDateTime completedAt,

	@Schema(description = "판매자 정산 완료 일시", example = "2026-05-06T22:05:00+09:00", nullable = true)
	OffsetDateTime settledAt
) {
}
