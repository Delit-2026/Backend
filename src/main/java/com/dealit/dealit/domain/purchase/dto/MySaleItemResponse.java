package com.dealit.dealit.domain.purchase.dto;

import com.dealit.dealit.domain.purchase.entity.PurchaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "마이페이지 판매내역 목록 아이템")
public record MySaleItemResponse(
	@Schema(description = "구매 ID", example = "1")
	Long purchaseId,

	@Schema(description = "상품 ID", example = "10")
	Long productId,

	@Schema(description = "상품명", example = "아이폰 14 Pro")
	String productTitle,

	@Schema(description = "상품 대표 이미지 URL", example = "http://localhost:8080/uploads/product/images/1-sample.jpg", nullable = true)
	String productImageUrl,

	@Schema(description = "거래 상대 회원 ID. 판매내역에서는 구매자 ID입니다.", example = "2")
	Long counterpartMemberId,

	@Schema(description = "결제 금액", example = "30000")
	long amount,

	@Schema(description = "구매 상태", example = "PAID", allowableValues = {"PAID", "SHIPPED", "COMPLETED", "CANCELED"})
	PurchaseStatus status,

	@Schema(description = "구매 일시", example = "2026-05-06T21:30:00+09:00")
	OffsetDateTime purchasedAt,

	@Schema(description = "연결된 채팅방 ID. 없으면 null입니다.", example = "15", nullable = true)
	Long chatRoomId
) {
}
