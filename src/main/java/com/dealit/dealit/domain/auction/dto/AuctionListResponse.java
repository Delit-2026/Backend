package com.dealit.dealit.domain.auction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "경매 상품 목록 응답")
public record AuctionListResponse(
	@Schema(description = "경매 상품 목록")
	List<AuctionListItemResponse> content
) {
}
