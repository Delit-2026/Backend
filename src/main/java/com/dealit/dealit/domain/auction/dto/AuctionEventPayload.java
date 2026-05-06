package com.dealit.dealit.domain.auction.dto;

import com.dealit.dealit.domain.auction.AuctionStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class AuctionEventPayload {

	private AuctionEventPayload() {
	}

	public record BidUpdated(
		String type,
		Long auctionId,
		BigDecimal currentPrice,
		Long bidderId,
		OffsetDateTime serverTime
	) {
		public BidUpdated(Long auctionId, BigDecimal currentPrice, Long bidderId, OffsetDateTime serverTime) {
			this("BID_UPDATED", auctionId, currentPrice, bidderId, serverTime);
		}
	}

	public record Outbid(
		String type,
		Long auctionId,
		Long previousBidderId,
		Long newBidderId,
		BigDecimal currentPrice,
		OffsetDateTime serverTime
	) {
		public Outbid(
			Long auctionId,
			Long previousBidderId,
			Long newBidderId,
			BigDecimal currentPrice,
			OffsetDateTime serverTime
		) {
			this("OUTBID", auctionId, previousBidderId, newBidderId, currentPrice, serverTime);
		}
	}

	public record AuctionEnded(
		String type,
		Long auctionId,
		Long winnerId,
		BigDecimal finalPrice,
		AuctionStatus status,
		OffsetDateTime serverTime
	) {
		public AuctionEnded(
			Long auctionId,
			Long winnerId,
			BigDecimal finalPrice,
			AuctionStatus status,
			OffsetDateTime serverTime
		) {
			this("AUCTION_ENDED", auctionId, winnerId, finalPrice, status, serverTime);
		}
	}

	public record BidReceived(
		String type,
		Long auctionId,
		Long bidderId,
		BigDecimal currentPrice,
		long bidCount,
		boolean firstBid,
		OffsetDateTime serverTime
	) {
		public BidReceived(
			Long auctionId,
			Long bidderId,
			BigDecimal currentPrice,
			long bidCount,
			boolean firstBid,
			OffsetDateTime serverTime
		) {
			this("BID_RECEIVED", auctionId, bidderId, currentPrice, bidCount, firstBid, serverTime);
		}
	}

	public record AuctionBidUpdated(
		String type,
		Long auctionId,
		BigDecimal currentPrice,
		BigDecimal minimumNextBidPrice,
		long bidCount,
		long bidderCount,
		OffsetDateTime serverTime
	) {
		public AuctionBidUpdated(
			Long auctionId,
			BigDecimal currentPrice,
			BigDecimal minimumNextBidPrice,
			long bidCount,
			long bidderCount,
			OffsetDateTime serverTime
		) {
			this("AUCTION_BID_UPDATED", auctionId, currentPrice, minimumNextBidPrice, bidCount, bidderCount, serverTime);
		}
	}
}
