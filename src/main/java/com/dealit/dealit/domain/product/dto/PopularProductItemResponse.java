package com.dealit.dealit.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "실시간 인기 일반 상품 항목")
public record PopularProductItemResponse(
	@Schema(description = "상품 ID", example = "1")
	Long productId,

	@Schema(description = "상품명", example = "맥북에어입니다.")
	String name,

	@Schema(description = "대표 이미지 URL", example = "http://localhost:8080/uploads/product/images/1.jpg")
	String thumbnailUrl,

	@Schema(description = "가격", example = "30000")
	BigDecimal price,

	@Schema(description = "거래 위치", example = "서울 강남구")
	String location,

	@Schema(description = "카테고리명", example = "디지털기기")
	String categoryName,

	@Schema(description = "조회수", example = "120")
	long viewCount,

	@Schema(description = "등록 시각", example = "2026-05-03T12:00:00+09:00")
	OffsetDateTime createdAt,

	@Schema(description = "실시간 인기 점수", example = "30.0")
	double popularScore
) {
}
