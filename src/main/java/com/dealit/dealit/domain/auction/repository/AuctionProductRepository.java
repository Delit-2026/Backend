package com.dealit.dealit.domain.auction.repository;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.entity.AuctionProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuctionProductRepository extends JpaRepository<AuctionProduct, Long> {
	List<AuctionProduct> findAllByMemberIdAndDeletedAtIsNullAndStatusOrderByCreatedAtDesc(Long memberId, AuctionStatus status);

	Optional<AuctionProduct> findByProductIdAndDeletedAtIsNull(Long productId);
}
