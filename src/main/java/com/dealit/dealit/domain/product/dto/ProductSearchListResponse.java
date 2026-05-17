package com.dealit.dealit.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "상품 검색 목록 응답")
public record ProductSearchListResponse(
	@Schema(description = "검색 결과")
	List<ProductSearchItemResponse> content,

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
