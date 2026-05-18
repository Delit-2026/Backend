package com.dealit.dealit.domain.auction.dto;

import com.dealit.dealit.domain.auction.AuctionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "재경매 거절 응답")
public record DeclineReauctionResponse(
	Long auctionId,
	AuctionStatus status
) {
}
