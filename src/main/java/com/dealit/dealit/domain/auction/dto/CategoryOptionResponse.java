package com.dealit.dealit.domain.auction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "카테고리 선택 옵션")
public record CategoryOptionResponse(
	@Schema(description = "카테고리 ID", example = "1")
	Long id,

	@Schema(description = "카테고리 한글명", example = "전자기기")
	String nameKo,

	@Schema(description = "카테고리 영문명", example = "Electronics")
	String nameEn,

	@Schema(description = "카테고리 깊이", example = "1")
	Integer depth,

	@Schema(description = "부모 카테고리 ID", example = "null", nullable = true)
	Long parentId,

	@Schema(description = "하위 카테고리 목록")
	List<CategoryOptionResponse> children
) {
}
