package com.dealit.dealit.domain.product.dto;

import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Objects;

@Schema(description = "일반 상품 등록 응답")
public record CreateProductResponse(
	@Schema(description = "등록된 상품 ID", example = "123")
	Long productId,

	@Schema(description = "판매 유형", example = "REGULAR")
	ProductSaleType saleType,

	@Schema(description = "상품 상태", example = "ON_SALE")
	ProductStatus status,

	@Schema(description = "경매 메타 정보", nullable = true)
	AuctionResponse auction,

	@Schema(description = "일반 판매 정보")
	GeneralSaleResponse generalSale
) {
	public record AuctionResponse() {
	}

	public record GeneralSaleResponse(
		@Schema(description = "일반 판매가", example = "12000")
		BigDecimal price
	) {
		public GeneralSaleResponse {
			Objects.requireNonNull(price, "generalSale.price must not be null");
		}
	}
}
