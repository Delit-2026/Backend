package com.dealit.dealit.domain.auction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "카테고리 추천 요청")
public record RecommendCategoryRequest(
	@Schema(description = "상품명", example = "닌텐도 스위치")
	@NotBlank(message = "상품명은 필수입니다.")
	@Size(max = 100, message = "상품명은 100자 이하여야 합니다.")
	String name,

	@Schema(description = "상품 설명", example = "독과 조이콘이 포함된 중고 제품입니다.")
	@NotBlank(message = "상품 설명은 필수입니다.")
	@Size(max = 2000, message = "상품 설명은 2000자 이하여야 합니다.")
	String description,

	@Schema(description = "사용자가 선택한 대분류 카테고리 ID", example = "1")
	@NotNull(message = "대분류 카테고리는 필수입니다.")
	Long topCategoryId,

	@Schema(description = "추천에 참고할 상품 이미지 URL 목록")
	@Size(max = 5, message = "이미지는 최대 5개까지 사용할 수 있습니다.")
	List<String> imageUrls
) {
}
