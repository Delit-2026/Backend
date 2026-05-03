package com.dealit.dealit.domain.product.dto;

import com.dealit.dealit.domain.product.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "내 판매 중 일반 상품 항목")
public record MySellingProductItemResponse(
	@Schema(description = "상품 ID", example = "10")
	Long productId,

	@Schema(description = "상품명", example = "아이폰 14 Pro")
	String name,

	@Schema(description = "상품 설명", example = "거의 새 상품입니다.")
	String description,

	@Schema(description = "카테고리명", example = "전자기기")
	String categoryName,

	@Schema(description = "대표 이미지 URL", example = "https://example.com/product/images/10.jpg")
	String thumbnailUrl,

	@Schema(description = "상품 상태", example = "ON_SALE")
	ProductStatus productStatus,

	@Schema(description = "판매 가격", example = "850000")
	BigDecimal price,

	@Schema(description = "거래 위치", example = "서울특별시 강남구")
	String location,

	@Schema(description = "수정 가능 여부", example = "true")
	boolean canEdit,

	@Schema(description = "삭제 가능 여부", example = "true")
	boolean canDelete,

	@Schema(description = "생성 시각", example = "2026-05-02T10:00:00+09:00")
	OffsetDateTime createdAt,

	@Schema(description = "수정 시각", example = "2026-05-02T10:30:00+09:00")
	OffsetDateTime updatedAt
) {
}
