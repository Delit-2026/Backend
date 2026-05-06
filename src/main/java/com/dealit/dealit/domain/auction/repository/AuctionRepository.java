package com.dealit.dealit.domain.auction.repository;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.entity.Auction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

	@EntityGraph(attributePaths = {"product", "product.images"})
	Page<Auction> findAllByProductMemberIdAndStatusAndDeletedAtIsNullAndProductDeletedAtIsNull(
		Long memberId,
		AuctionStatus status,
		Pageable pageable
	);

	@EntityGraph(attributePaths = {"product", "product.images"})
	Optional<Auction> findByAuctionIdAndProductMemberIdAndDeletedAtIsNullAndProductDeletedAtIsNull(
		Long auctionId,
		Long memberId
	);

	@EntityGraph(attributePaths = {"product", "product.images"})
	Optional<Auction> findByProductProductIdAndDeletedAtIsNullAndProductDeletedAtIsNull(Long productId);

	@EntityGraph(attributePaths = {"product"})
	Optional<Auction> findByAuctionIdAndDeletedAtIsNullAndProductDeletedAtIsNull(Long auctionId);

	@EntityGraph(attributePaths = {"product", "product.images"})
	Optional<Auction> findDetailByAuctionIdAndDeletedAtIsNullAndProductDeletedAtIsNull(Long auctionId);

	@Query("""
		select a as auction, count(b) as bidCount, max(b.createdAt) as latestBidAt
		from Auction a
		left join Bid b on b.auction = a and b.deletedAt is null
		where a.status = :status
			and a.endsAt > :now
			and a.deletedAt is null
			and a.product.deletedAt is null
		group by a
		""")
	List<AuctionRankProjection> findOngoingAuctionRanks(
		@Param("status") AuctionStatus status,
		@Param("now") OffsetDateTime now
	);

	@Query("""
		select a as auction, count(b) as bidCount, max(b.createdAt) as latestBidAt
		from Auction a
		left join Bid b on b.auction = a and b.deletedAt is null
		where a.status = :status
			and a.endsAt > :now
			and a.deletedAt is null
			and a.product.deletedAt is null
		group by a
		order by a.endsAt asc, count(b) desc, a.createdAt desc, a.auctionId desc
		""")
	List<AuctionRankProjection> findClosingSoonAuctionRanks(
		@Param("status") AuctionStatus status,
		@Param("now") OffsetDateTime now,
		Pageable pageable
	);
}
