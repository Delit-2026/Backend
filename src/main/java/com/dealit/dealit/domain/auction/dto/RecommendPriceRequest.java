package com.dealit.dealit.domain.auction.dto;

import com.dealit.dealit.domain.auction.ProductSaleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "가격 추천 요청")
public record RecommendPriceRequest(
	@Schema(description = "상품명", example = "Nintendo Switch OLED")
	@NotBlank(message = "name is required.")
	@Size(max = 100, message = "name must be 100 characters or fewer.")
	String name,

	@Schema(description = "상품 설명", example = "Used console with dock and controllers.")
	@NotBlank(message = "description is required.")
	@Size(max = 2000, message = "description must be 2000 characters or fewer.")
	String description,

	@Schema(description = "판매 유형", example = "REGULAR")
	@NotNull(message = "saleType is required.")
	ProductSaleType saleType
) {
}
