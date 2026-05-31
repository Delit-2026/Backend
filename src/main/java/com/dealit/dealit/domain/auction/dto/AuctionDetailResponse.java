package com.dealit.dealit.domain.auction.dto;

import com.dealit.dealit.domain.auction.AuctionStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record AuctionDetailResponse(
	Long auctionId,
	Long productId,
	String name,
	String description,
	Long categoryId,
	String categoryName,
	String location,
	List<ImageResponse> images,
	SellerResponse seller,
	BigDecimal startPrice,
	BigDecimal currentPrice,
	BigDecimal minimumBidAmount,
	BigDecimal minimumNextBidPrice,
	int bidCount,
	int bidderCount,
	OffsetDateTime startAt,
	OffsetDateTime endsAt,
	OffsetDateTime serverTime,
	AuctionStatus status,
	boolean liked,
	long favoriteCount
) {
	public record ImageResponse(
		Long imageId,
		String imageUrl,
		Integer sortOrder
	) {
	}

	public record SellerResponse(
		Long memberId,
		String nickname,
		String profileImageUrl,
		double rating
	) {
	}
}
