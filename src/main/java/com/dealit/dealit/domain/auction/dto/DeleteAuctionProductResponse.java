package com.dealit.dealit.domain.auction.dto;

public record DeleteAuctionProductResponse(
	Long productId,
	boolean deleted
) {
}
