package com.dealit.dealit.domain.auction.dto;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.ProductSaleType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "경매 상품 등록 응답")
public record CreateAuctionResponse(
	@Schema(description = "등록된 상품 ID", example = "1001")
	Long productId,

	@Schema(description = "판매 유형", example = "AUCTION")
	ProductSaleType saleType,

	@Schema(description = "상품 상태", example = "AUCTION_LIVE")
	AuctionStatus status
) {
}
