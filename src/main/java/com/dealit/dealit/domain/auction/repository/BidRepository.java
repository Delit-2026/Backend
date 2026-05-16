package com.dealit.dealit.domain.auction.repository;

import com.dealit.dealit.domain.auction.entity.Bid;
import java.util.Collection;
import java.util.List;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidRepository extends JpaRepository<Bid, Long> {

	boolean existsByAuctionAuctionId(Long auctionId);

	boolean existsByAuctionAuctionIdAndBidderIdAndDeletedAtIsNull(Long auctionId, Long bidderId);

	long countByAuctionAuctionId(Long auctionId);

	List<Bid> findAllByAuctionAuctionIdOrderByCreatedAtDescBidIdDesc(Long auctionId);

	@Query("""
		select distinct b.bidderId
		from Bid b
		where b.auction.auctionId = :auctionId
		  and b.deletedAt is null
		""")
	List<Long> findDistinctBidderIdsByAuctionId(@Param("auctionId") Long auctionId);

	@Query("""
		select distinct b.bidderId
		from Bid b
		where b.auction.auctionId = :auctionId
		  and b.bidderId <> :winnerId
		  and b.deletedAt is null
		""")
	List<Long> findDistinctBidderIdsByAuctionIdAndBidderIdNot(
		@Param("auctionId") Long auctionId,
		@Param("winnerId") Long winnerId
	);

	@Query("select count(distinct b.bidderId) from Bid b where b.auction.auctionId = :auctionId")
	long countDistinctBidderIdByAuctionId(Long auctionId);

	@Query("""
		select b.auction.auctionId as auctionId,
			count(b) as bidCount
		from Bid b
		where b.auction.auctionId in :auctionIds
			and b.deletedAt is null
		group by b.auction.auctionId
		""")
	List<AuctionBidCountProjection> countByAuctionIds(@Param("auctionIds") Collection<Long> auctionIds);

	@Query(
		value = """
			select
				b.auction_id as auctionId,
				max(b.bid_price) as myBidAmount,
				max(b.created_at) as lastBidAt,
				(
					select b2.bidder_id
					from bid b2
					where b2.auction_id = b.auction_id
						and b2.deleted_at is null
					order by b2.bid_price desc, b2.created_at desc, b2.bid_id desc
					limit 1
				) as highestBidderId,
				(
					select b2.bid_price
					from bid b2
					where b2.auction_id = b.auction_id
						and b2.deleted_at is null
					order by b2.bid_price desc, b2.created_at desc, b2.bid_id desc
					limit 1
				) as highestBidAmount
			from bid b
			join auction a on a.auction_id = b.auction_id
			join product p on p.product_id = a.product_id
			where b.bidder_id = :memberId
				and b.deleted_at is null
				and a.deleted_at is null
				and p.deleted_at is null
				and not exists (
					select 1
					from buying_auction_hidden h
					where h.member_id = :memberId
						and h.auction_id = b.auction_id
						and h.deleted_at is null
				)
			group by b.auction_id, a.status, a.ends_at
			order by
				case when a.status = 'ONGOING' and a.ends_at > :now then 0 else 1 end asc,
				case when a.status = 'ONGOING' and a.ends_at > :now then a.ends_at end asc,
				max(b.created_at) desc,
				b.auction_id desc
			""",
		countQuery = """
			select count(*)
			from (
				select b.auction_id
				from bid b
				join auction a on a.auction_id = b.auction_id
				join product p on p.product_id = a.product_id
				where b.bidder_id = :memberId
					and b.deleted_at is null
					and a.deleted_at is null
					and p.deleted_at is null
					and not exists (
						select 1
						from buying_auction_hidden h
						where h.member_id = :memberId
							and h.auction_id = b.auction_id
							and h.deleted_at is null
					)
				group by b.auction_id
			) buying_auctions
			""",
		nativeQuery = true
	)
	Page<MyBuyingAuctionProjection> findMyBuyingAuctions(
		@Param("memberId") Long memberId,
		@Param("now") OffsetDateTime now,
		Pageable pageable
	);
}
