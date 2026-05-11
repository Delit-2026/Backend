package com.dealit.dealit.domain.auction.dto;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.BuyingAuctionStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MyBuyingAuctionItemResponse(
	Long productId,
	Long auctionId,
	String name,
	String thumbnailUrl,
	String categoryName,
	String location,
	BigDecimal myBidAmount,
	BigDecimal currentHighestBidAmount,
	BuyingAuctionStatus buyingStatus,
	AuctionStatus auctionStatus,
	int bidCount,
	int bidderCount,
	OffsetDateTime endsAt,
	OffsetDateTime lastBidAt
) {
}
