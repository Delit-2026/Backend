package com.dealit.dealit.domain.auction.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface MyBuyingAuctionProjection {

	Long getAuctionId();

	BigDecimal getMyBidAmount();

	LocalDateTime getLastBidAt();

	Long getHighestBidderId();

	BigDecimal getHighestBidAmount();
}
