package com.dealit.dealit.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "통합 검색 목록 응답")
public record SearchListResponse(
	@Schema(description = "검색어", example = "맥북")
	String keyword,

	@Schema(description = "검색 결과 유형 필터", example = "REGULAR")
	SearchResultType type,

	@Schema(description = "카테고리 ID", example = "1")
	Long categoryId,

	@Schema(description = "검색 결과")
	List<SearchItemResponse> content,

	@Schema(description = "현재 페이지", example = "0")
	int page,

	@Schema(description = "페이지 크기", example = "20")
	int size,

	@Schema(description = "전체 개수", example = "42")
	long totalElements,

	@Schema(description = "다음 페이지 존재 여부", example = "true")
	boolean hasNext
) {
}
