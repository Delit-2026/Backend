package com.dealit.dealit.domain.product.dto;

import com.dealit.dealit.domain.product.ProductSaleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "일반 상품 가격 추천 요청")
public record RecommendPriceRequest(
	@Schema(description = "상품명", example = "닌텐도 스위치 OLED")
	@NotBlank(message = "상품명은 필수입니다.")
	@Size(max = 100, message = "상품명은 100자 이하여야 합니다.")
	String name,

	@Schema(description = "상품 설명", example = "사용하지 않아서 깨끗합니다.")
	@NotBlank(message = "상품 설명은 필수입니다.")
	@Size(max = 2000, message = "상품 설명은 2000자 이하여야 합니다.")
	String description,

	@Schema(description = "판매 유형", example = "REGULAR")
	@NotNull(message = "판매 유형은 필수입니다.")
	ProductSaleType saleType
) {
}
