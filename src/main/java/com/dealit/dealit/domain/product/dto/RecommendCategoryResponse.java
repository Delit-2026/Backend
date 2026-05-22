package com.dealit.dealit.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "일반 상품 카테고리 추천 응답")
public record RecommendCategoryResponse(
	@Schema(description = "추천 카테고리 ID", example = "200")
	Long categoryId,

	@Schema(description = "추천 카테고리명", example = "Digital/Electronics")
	String categoryName,

	@Schema(description = "대분류부터 추천 카테고리까지의 카테고리 ID 경로", example = "[1, 4, 16]")
	List<Long> categoryPathIds,

	@Schema(description = "대분류부터 추천 카테고리까지의 카테고리명 경로", example = "[\"전자기기\", \"스마트폰\", \"아이폰\"]")
	List<String> categoryNames,

	@Schema(description = "AI 추천 신뢰도", example = "0.91")
	Double confidence,

	@Schema(description = "AI 추천 사유")
	String reason
) {
}
