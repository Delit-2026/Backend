package com.dealit.dealit.domain.auction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "카테고리 추천 요청")
public record RecommendCategoryRequest(
	@Schema(description = "상품명", example = "닌텐도 스위치")
	@NotBlank(message = "상품명은 필수입니다.")
	@Size(max = 100, message = "상품명은 100자 이하여야 합니다.")
	String name,

	@Schema(description = "상품 설명", example = "독과 조이콘이 포함된 중고 제품입니다.")
	@NotBlank(message = "상품 설명은 필수입니다.")
	@Size(max = 2000, message = "상품 설명은 2000자 이하여야 합니다.")
	String description
) {
}
