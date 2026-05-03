package com.dealit.dealit.domain.auction.dto;

import com.dealit.dealit.domain.auction.AuctionStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class AuctionEventPayload {

	private AuctionEventPayload() {
	}

	public record BidUpdated(
		Long auctionId,
		BigDecimal currentPrice,
		Long bidderId,
		OffsetDateTime serverTime
	) {
	}

	public record Outbid(
		Long auctionId,
		Long previousBidderId,
		Long newBidderId,
		BigDecimal currentPrice,
		OffsetDateTime serverTime
	) {
	}

	public record AuctionEnded(
		Long auctionId,
		Long winnerId,
		BigDecimal finalPrice,
		AuctionStatus status,
		OffsetDateTime serverTime
	) {
	}
}
