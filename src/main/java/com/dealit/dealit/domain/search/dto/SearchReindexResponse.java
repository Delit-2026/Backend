package com.dealit.dealit.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "검색 인덱스 재생성 응답")
public record SearchReindexResponse(
	@Schema(description = "OpenSearch 인덱스명", example = "dealit-search")
	String indexName,

	@Schema(description = "색인된 문서 개수", example = "25")
	int indexedCount
) {
}
