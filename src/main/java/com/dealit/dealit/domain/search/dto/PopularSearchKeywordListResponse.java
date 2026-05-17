package com.dealit.dealit.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "인기 검색어 목록 응답")
public record PopularSearchKeywordListResponse(
	@Schema(description = "인기 검색어 목록")
	List<PopularSearchKeywordResponse> content,

	@Schema(description = "조회 개수", example = "10")
	int size
) {
}
