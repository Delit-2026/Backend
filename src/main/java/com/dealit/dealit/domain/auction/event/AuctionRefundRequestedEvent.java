package com.dealit.dealit.domain.auction.event;

public record AuctionRefundRequestedEvent(
	Long auctionId,
	Long bidderId,
	Long paymentId,
	Long amount
) {
}
