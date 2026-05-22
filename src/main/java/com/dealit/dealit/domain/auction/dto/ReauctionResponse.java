package com.dealit.dealit.domain.auction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "재경매 등록 응답")
public record ReauctionResponse(
	Long sourceAuctionId,
	Long productId,
	Long auctionId
) {
}
