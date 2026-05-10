package com.dealit.dealit.domain.auction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AuctionBidHistoryResponse(
	Long auctionId,
	BigDecimal currentPrice,
	long bidCount,
	List<BidHistoryItem> bids
) {

	public record BidHistoryItem(
		Long bidId,
		Long bidderId,
		String bidderNickname,
		String bidderProfileImageUrl,
		BigDecimal bidPrice,
		LocalDateTime bidAt,
		boolean highest
	) {
	}
}
