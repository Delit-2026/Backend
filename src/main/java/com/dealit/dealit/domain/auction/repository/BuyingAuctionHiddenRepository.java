package com.dealit.dealit.domain.auction.repository;

import com.dealit.dealit.domain.auction.entity.BuyingAuctionHidden;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyingAuctionHiddenRepository extends JpaRepository<BuyingAuctionHidden, Long> {

	boolean existsByMemberIdAndAuctionIdAndDeletedAtIsNull(Long memberId, Long auctionId);
}
