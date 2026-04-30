package com.dealit.dealit.domain.auction.repository;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.entity.AuctionProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuctionProductRepository extends JpaRepository<AuctionProduct, Long> {

	@EntityGraph(attributePaths = "images")
	Page<AuctionProduct> findAllByMemberIdAndStatusAndDeletedAtIsNull(
		Long memberId,
		AuctionStatus status,
		Pageable pageable
	);

	@EntityGraph(attributePaths = "images")
	Optional<AuctionProduct> findByProductIdAndMemberIdAndDeletedAtIsNull(Long productId, Long memberId);
}
