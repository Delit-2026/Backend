package com.dealit.dealit.domain.purchase.dto;

import com.dealit.dealit.domain.purchase.entity.PurchaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "구매 거래 상태 변경 응답")
public record PurchaseCompletionResponse(
	@Schema(description = "구매 ID", example = "1")
	Long purchaseId,

	@Schema(description = "구매 상태", example = "COMPLETED", allowableValues = {"PAID", "SHIPPED", "COMPLETED", "CANCELED"})
	PurchaseStatus status,

	@Schema(description = "구매자 수령확정 여부", example = "true")
	boolean buyerCompleted,

	@Schema(description = "판매자 발송완료 여부", example = "true")
	boolean sellerCompleted,

	@Schema(description = "구매자 수령확정 일시", example = "2026-05-06T22:00:00+09:00", nullable = true)
	OffsetDateTime buyerCompletedAt,

	@Schema(description = "판매자 발송완료 일시", example = "2026-05-06T22:05:00+09:00", nullable = true)
	OffsetDateTime sellerCompletedAt,

	@Schema(description = "판매자 발송완료 일시", example = "2026-05-06T22:05:00+09:00", nullable = true)
	OffsetDateTime shippedAt,

	@Schema(description = "자동 수령확정 예정 일시", example = "2026-05-13T22:05:00+09:00", nullable = true)
	OffsetDateTime autoCompleteAt,

	@Schema(description = "거래 완료 일시", example = "2026-05-06T22:05:00+09:00", nullable = true)
	OffsetDateTime completedAt,

	@Schema(description = "거래 취소 일시", example = "2026-05-09T22:05:00+09:00", nullable = true)
	OffsetDateTime canceledAt,

	@Schema(description = "판매자 정산 완료 일시", example = "2026-05-06T22:05:00+09:00", nullable = true)
	OffsetDateTime settledAt
) {
}
