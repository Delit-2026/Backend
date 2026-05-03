package com.dealit.dealit.domain.auction.event;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.dto.AuctionEventPayload;
import com.dealit.dealit.global.event.service.EventStreamService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
		if (previousBidderId == null) {
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
}
