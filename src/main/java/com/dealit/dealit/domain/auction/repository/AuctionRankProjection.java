package com.dealit.dealit.domain.auction.repository;

import com.dealit.dealit.domain.auction.entity.Auction;

import java.time.LocalDateTime;

public interface AuctionRankProjection {

	Auction getAuction();

	long getBidCount();

	LocalDateTime getLatestBidAt();
}
