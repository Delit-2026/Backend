package com.dealit.dealit.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "일반 상품 가격 추천 응답")
public record RecommendPriceResponse(
	@Schema(description = "추천 판매가", example = "250000")
	BigDecimal recommendedPrice
) {
}
