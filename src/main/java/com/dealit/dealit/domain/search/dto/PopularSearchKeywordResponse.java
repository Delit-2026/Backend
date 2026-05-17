package com.dealit.dealit.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인기 검색어 항목")
public record PopularSearchKeywordResponse(
	@Schema(description = "검색어", example = "맥북")
	String keyword,

	@Schema(description = "검색 횟수", example = "12")
	long count
) {
}
