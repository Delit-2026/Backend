package com.dealit.dealit.domain.auction.repository;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.entity.Auction;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
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

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"product"})
	Optional<Auction> findWithLockByAuctionIdAndDeletedAtIsNullAndProductDeletedAtIsNull(Long auctionId);

	@EntityGraph(attributePaths = {"product", "product.images"})
	Optional<Auction> findDetailByAuctionIdAndDeletedAtIsNullAndProductDeletedAtIsNull(Long auctionId);

	@EntityGraph(attributePaths = {"product", "product.images"})
	List<Auction> findAllByAuctionIdInAndDeletedAtIsNullAndProductDeletedAtIsNull(Collection<Long> auctionIds);

	@EntityGraph(attributePaths = {"product", "product.images"})
	List<Auction> findAllByProductProductIdInAndDeletedAtIsNullAndProductDeletedAtIsNull(Collection<Long> productIds);

	@Query("""
		select distinct a.product.categoryId
		from Auction a
		where a.status = :status
			and a.endsAt > :now
			and a.deletedAt is null
			and a.product.deletedAt is null
		""")
	List<Long> findDistinctCategoryIdsByStatusAndEndsAtAfterAndDeletedAtIsNullAndProductDeletedAtIsNull(
		@Param("status") AuctionStatus status,
		@Param("now") OffsetDateTime now
	);

	@EntityGraph(attributePaths = {"product", "product.images"})
	Page<Auction> findAllByStatusAndEndsAtAfterAndDeletedAtIsNullAndProductDeletedAtIsNullAndProductCategoryIdIn(
		AuctionStatus status,
		OffsetDateTime now,
		Collection<Long> categoryIds,
		Pageable pageable
	);

	@EntityGraph(attributePaths = {"product"})
	List<Auction> findAllByStatusAndEndsAtBetweenAndDeletedAtIsNullAndProductDeletedAtIsNull(
		AuctionStatus status,
		OffsetDateTime startsAt,
		OffsetDateTime endsAt
	);

	@Query(value = """
		select
			a.auction_id as auctionId,
			count(b.bid_id) as bidCount,
			max(b.created_at) as latestBidAt,
			cast(count(b.bid_id) as double precision)
				/ greatest(1.0, extract(epoch from (cast(:now as timestamp) - p.created_at)) / 3600.0) as popularScore
		from auction a
		join product p on p.product_id = a.product_id
		left join bid b on b.auction_id = a.auction_id and b.deleted_at is null
		where a.status = :status
			and a.ends_at > :now
			and a.deleted_at is null
			and p.deleted_at is null
		group by a.auction_id, p.created_at
		order by popularScore desc,
			count(b.bid_id) desc,
			max(b.created_at) desc nulls last,
			p.created_at desc,
			a.auction_id desc
		""", nativeQuery = true)
	List<AuctionRankProjection> findOngoingAuctionRanks(
		@Param("status") String status,
		@Param("now") OffsetDateTime now,
		Pageable pageable
	);

	@Query(value = """
		select
			a.auction_id as auctionId,
			count(b.bid_id) as bidCount,
			max(b.created_at) as latestBidAt,
			cast(count(b.bid_id) as double precision)
				/ greatest(1.0, extract(epoch from (cast(:now as timestamp) - p.created_at)) / 3600.0) as popularScore
		from auction a
		join product p on p.product_id = a.product_id
		left join bid b on b.auction_id = a.auction_id and b.deleted_at is null
		where a.status = :status
			and a.ends_at > :now
			and a.deleted_at is null
			and p.deleted_at is null
		group by a.auction_id, p.created_at
		order by a.ends_at asc,
			count(b.bid_id) desc,
			p.created_at desc,
			a.auction_id desc
		""", nativeQuery = true)
	List<AuctionRankProjection> findClosingSoonAuctionRanks(
		@Param("status") String status,
		@Param("now") OffsetDateTime now,
		Pageable pageable
	);
}
