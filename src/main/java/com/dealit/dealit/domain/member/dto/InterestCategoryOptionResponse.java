package com.dealit.dealit.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관심 카테고리 선택지")
public record InterestCategoryOptionResponse(
	@Schema(description = "카테고리 ID", example = "1")
	Long categoryId,

	@Schema(description = "카테고리명(한글)", example = "전자기기")
	String nameKo,

	@Schema(description = "카테고리명(영문)", example = "electronics")
	String nameEn
) {
}
