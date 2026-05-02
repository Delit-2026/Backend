package com.dealit.dealit.domain.auction.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BidResponse(
	Long auctionId,
	BigDecimal currentPrice,
	Long bidderId,
	OffsetDateTime serverTime
) {
}
