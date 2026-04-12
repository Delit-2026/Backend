package com.dealit.dealit.domain.auction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "카테고리 추천 요청")
public record RecommendCategoryRequest(
	@Schema(description = "상품명", example = "Nintendo Switch OLED")
	@NotBlank(message = "name is required.")
	@Size(max = 100, message = "name must be 100 characters or fewer.")
	String name,

	@Schema(description = "상품 설명", example = "Used console with dock and controllers.")
	@NotBlank(message = "description is required.")
	@Size(max = 2000, message = "description must be 2000 characters or fewer.")
	String description
) {
}
