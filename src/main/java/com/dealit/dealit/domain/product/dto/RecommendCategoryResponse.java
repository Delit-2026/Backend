package com.dealit.dealit.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일반 상품 카테고리 추천 응답")
public record RecommendCategoryResponse(
	@Schema(description = "추천 카테고리 ID", example = "200")
	Long categoryId,

	@Schema(description = "추천 카테고리명", example = "Digital/Electronics")
	String categoryName
) {
}
