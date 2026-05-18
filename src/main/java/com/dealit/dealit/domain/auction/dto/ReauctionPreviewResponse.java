package com.dealit.dealit.domain.auction.dto;

import com.dealit.dealit.domain.auction.AuctionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "재경매 미리보기 응답")
public record ReauctionPreviewResponse(
	Long productId,
	Long auctionId,
	String name,
	String description,
	Long categoryId,
	String categoryName,
	String location,
	List<AuctionEditDetailResponse.AuctionEditImageResponse> images,
	BigDecimal startPrice,
	BigDecimal minimumBidAmount,
	long auctionDurationDays,
	AuctionStatus auctionStatus,
	OffsetDateTime noBidAt,
	OffsetDateTime reauctionExpiresAt,
	long remainingSeconds
) {
}
