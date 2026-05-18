package com.dealit.dealit.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "통합 검색 결과 유형")
public enum SearchResultType {
	REGULAR,
	AUCTION
}
