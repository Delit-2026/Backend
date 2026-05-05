package com.dealit.dealit.domain.auction.event;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.dto.AuctionEventPayload;
import com.dealit.dealit.global.event.service.EventStreamService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuctionEventPublisher {

	private final EventStreamService eventStreamService;

	public void publishBidUpdated(Long auctionId, BigDecimal currentPrice, Long bidderId, OffsetDateTime serverTime) {
		eventStreamService.publishRemote(
			bidderId,
			"BID_UPDATED",
			new AuctionEventPayload.BidUpdated(auctionId, currentPrice, bidderId, serverTime)
		);
	}

	public void publishOutbid(
		Long auctionId,
		Long previousBidderId,
		Long newBidderId,
		BigDecimal currentPrice,
		OffsetDateTime serverTime
	) {
		if (previousBidderId == null || previousBidderId.equals(newBidderId)) {
			return;
		}
		eventStreamService.publishRemote(
			previousBidderId,
			"OUTBID",
			new AuctionEventPayload.Outbid(auctionId, previousBidderId, newBidderId, currentPrice, serverTime)
		);
	}

	public void publishAuctionEnded(
		Long receiverId,
		Long auctionId,
		Long winnerId,
		BigDecimal finalPrice,
		AuctionStatus status,
		OffsetDateTime serverTime
	) {
		if (receiverId == null) {
			return;
		}
		eventStreamService.publishRemote(
			receiverId,
			"AUCTION_ENDED",
			new AuctionEventPayload.AuctionEnded(auctionId, winnerId, finalPrice, status, serverTime)
		);
	}

	public void publishBidReceived(
		Long sellerId,
		Long auctionId,
		Long bidderId,
		BigDecimal currentPrice,
		long bidCount,
		boolean firstBid,
		OffsetDateTime serverTime
	) {
		if (sellerId == null) {
			return;
		}
		eventStreamService.publishRemote(
			sellerId,
			"BID_RECEIVED",
			new AuctionEventPayload.BidReceived(auctionId, bidderId, currentPrice, bidCount, firstBid, serverTime)
		);
	}

	public void publishAuctionBidUpdated(
		Collection<Long> receiverIds,
		Long auctionId,
		BigDecimal currentPrice,
		BigDecimal minimumNextBidPrice,
		long bidCount,
		long bidderCount,
		OffsetDateTime serverTime
	) {
		Set<Long> uniqueReceiverIds = new LinkedHashSet<>(receiverIds);
		for (Long receiverId : uniqueReceiverIds) {
			if (receiverId == null) {
				continue;
			}
			eventStreamService.publishRemote(
				receiverId,
				"AUCTION_BID_UPDATED",
				new AuctionEventPayload.AuctionBidUpdated(
					auctionId,
					currentPrice,
					minimumNextBidPrice,
					bidCount,
					bidderCount,
					serverTime
				)
			);
		}
	}
}
