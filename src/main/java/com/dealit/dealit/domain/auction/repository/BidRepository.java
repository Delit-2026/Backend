package com.dealit.dealit.domain.auction.repository;

import com.dealit.dealit.domain.auction.entity.Bid;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BidRepository extends JpaRepository<Bid, Long> {

	boolean existsByAuctionAuctionId(Long auctionId);

	long countByAuctionAuctionId(Long auctionId);

	List<Bid> findAllByAuctionAuctionIdOrderByCreatedAtDescBidIdDesc(Long auctionId);

	@Query("select count(distinct b.bidderId) from Bid b where b.auction.auctionId = :auctionId")
	long countDistinctBidderIdByAuctionId(Long auctionId);
}
