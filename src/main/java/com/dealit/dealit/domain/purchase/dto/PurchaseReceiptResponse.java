package com.dealit.dealit.domain.purchase.dto;

import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.purchase.entity.PurchaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "구매 영수증 응답")
public record PurchaseReceiptResponse(
	@Schema(description = "구매 ID", example = "1")
	Long purchaseId,

	@Schema(description = "상품 ID", example = "10")
	Long productId,

	@Schema(description = "상품명", example = "맥북 프로")
	String productTitle,

	@Schema(description = "상품 대표 이미지 URL", example = "http://localhost:8080/uploads/product/images/1-sample.jpg", nullable = true)
	String productImageUrl,

	@Schema(description = "구매자 회원 ID", example = "2")
	Long buyerId,

	@Schema(description = "판매자 회원 ID", example = "3")
	Long sellerId,

	@Schema(description = "결제 금액", example = "30000")
	long amount,

	@Schema(description = "구매 상태", example = "PAID", allowableValues = {"PAID", "SHIPPED", "COMPLETED", "CANCELED"})
	PurchaseStatus status,

	@Schema(description = "구매 일시", example = "2026-05-06T21:30:00+09:00")
	OffsetDateTime purchasedAt,

	@Schema(description = "연결된 채팅방 ID. 현재는 null일 수 있습니다.", example = "15", nullable = true)
	Long chatRoomId,

	@Schema(description = "상품 거래 유형", example = "REGULAR", allowableValues = {"REGULAR"})
	ProductSaleType productType,

	@Schema(description = "결제 완료 일시. 별도 값이 없으면 구매 일시와 같습니다.", example = "2026-05-06T21:30:00+09:00", nullable = true)
	OffsetDateTime paidAt,

	@Schema(description = "판매자 발송 완료 일시", example = "2026-05-06T22:05:00+09:00", nullable = true)
	OffsetDateTime shippedAt,

	@Schema(description = "거래 완료 일시", example = "2026-05-06T22:10:00+09:00", nullable = true)
	OffsetDateTime completedAt,

	@Schema(description = "거래 취소 일시", example = "2026-05-09T22:05:00+09:00", nullable = true)
	OffsetDateTime canceledAt,

	@Schema(description = "판매자 발송 완료 여부", example = "true")
	boolean sellerShipped,

	@Schema(description = "구매자 수령 확인 여부", example = "false")
	boolean buyerConfirmed,

	@Schema(description = "거래 상대 닉네임. 구매자에게는 판매자, 판매자에게는 구매자 닉네임을 반환합니다.", example = "Dealit#3", nullable = true)
	String counterpartNickname
) {
}
