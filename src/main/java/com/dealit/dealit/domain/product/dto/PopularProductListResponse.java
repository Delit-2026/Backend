package com.dealit.dealit.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "실시간 인기 일반 상품 목록 응답")
public record PopularProductListResponse(
	@Schema(description = "실시간 인기 일반 상품 목록")
	List<PopularProductItemResponse> content
) {
}
