package com.dealit.dealit.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "일반 상품 카테고리 응답")
public record CategoryOptionResponse(
	@Schema(description = "카테고리 ID", example = "1")
	Long id,

	@Schema(description = "카테고리명(한글)", example = "전자기기")
	String nameKo,

	@Schema(description = "카테고리명(영문)", example = "Electronics")
	String nameEn,

	@Schema(description = "깊이", example = "1")
	Integer depth,

	@Schema(description = "부모 카테고리 ID", example = "null", nullable = true)
	Long parentId,

	@Schema(description = "하위 카테고리")
	List<CategoryOptionResponse> children
) {
}
