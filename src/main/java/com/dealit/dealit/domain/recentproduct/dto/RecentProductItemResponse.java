package com.dealit.dealit.domain.recentproduct.dto;

import com.dealit.dealit.domain.recentproduct.RecentProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "최근 본 상품 항목")
public record RecentProductItemResponse(
	@Schema(description = "최근 본 상품 유형", example = "REGULAR")
	RecentProductType type,

	@Schema(description = "상품 ID", example = "1")
	Long productId,

	@Schema(description = "경매 ID", example = "10")
	Long auctionId,

	@Schema(description = "상품명", example = "맥북 에어")
	String name,

	@Schema(description = "대표 이미지 URL", example = "http://localhost:8080/uploads/product/images/example.jpg")
	String thumbnailUrl,

	@Schema(description = "일반 상품 가격", example = "30000")
	BigDecimal price,

	@Schema(description = "경매 현재가", example = "50000")
	BigDecimal currentPrice,

	@Schema(description = "거래 위치", example = "서울 강남구")
	String location,

	@Schema(description = "카테고리명", example = "노트북")
	String categoryName,

	@Schema(description = "조회 시각", example = "2026-05-21T12:00:00+09:00")
	OffsetDateTime viewedAt
) {
}
