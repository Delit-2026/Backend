package com.dealit.dealit.domain.auction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카테고리 추천 응답")
public record RecommendCategoryResponse(
	@Schema(description = "추천 카테고리 ID", example = "200")
	Long categoryId,

	@Schema(description = "추천 카테고리명", example = "디지털/가전")
	String categoryName
) {
}
