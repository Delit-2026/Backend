package com.dealit.dealit.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "핫한 일반 상품 목록 응답")
public record HotListProductListResponse(
	@Schema(description = "핫한 일반 상품 목록")
	List<HotListProductItemResponse> content
) {
}
