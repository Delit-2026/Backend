package com.dealit.dealit.domain.auction.repository;

import java.time.LocalDateTime;

public interface AuctionRankProjection {

	Long getAuctionId();

	Number getBidCount();

	LocalDateTime getLatestBidAt();

	Number getPopularScore();
}
