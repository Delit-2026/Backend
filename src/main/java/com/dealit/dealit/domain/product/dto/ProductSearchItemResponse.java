package com.dealit.dealit.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "상품 검색 항목")
public record ProductSearchItemResponse(
	@Schema(description = "상품 ID", example = "1")
	Long productId,

	@Schema(description = "상품명", example = "맥북 에어")
	String name,

	@Schema(description = "대표 이미지 URL", example = "http://localhost:8080/uploads/product/images/1.jpg")
	String thumbnailUrl,

	@Schema(description = "가격", example = "30000")
	BigDecimal price,

	@Schema(description = "거래 위치", example = "서울 강남구")
	String location,

	@Schema(description = "카테고리 ID", example = "19")
	Long categoryId,

	@Schema(description = "카테고리명", example = "맥북")
	String categoryName,

	@Schema(description = "조회수", example = "120")
	long viewCount,

	@Schema(description = "찜 수", example = "15")
	long favoriteCount,

	@Schema(description = "등록 시각", example = "2026-05-03T12:00:00+09:00")
	OffsetDateTime createdAt
) {
}
