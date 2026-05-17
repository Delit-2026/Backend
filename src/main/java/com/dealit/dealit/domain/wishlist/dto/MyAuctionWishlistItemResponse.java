package com.dealit.dealit.domain.wishlist.dto;

import com.dealit.dealit.domain.auction.AuctionStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MyAuctionWishlistItemResponse(
	Long auctionId,
	Long productId,
	String name,
	String thumbnailUrl,
	String categoryName,
	String location,
	long favoriteCount,
	BigDecimal currentPrice,
	int bidCount,
	AuctionStatus auctionStatus,
	OffsetDateTime endedAt,
	OffsetDateTime likedAt
) {
}
