package com.dealit.dealit.domain.review.repository;

import com.dealit.dealit.domain.review.dto.ReviewRatingSummaryResponse;
import com.dealit.dealit.domain.review.entity.Review;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	boolean existsByReviewerIdAndProductIdAndAuctionIdIsNullAndDeletedAtIsNull(Long reviewerId, Long productId);

	boolean existsByReviewerIdAndAuctionIdAndDeletedAtIsNull(Long reviewerId, Long auctionId);

	Page<Review> findAllByRevieweeIdAndDeletedAtIsNull(Long revieweeId, Pageable pageable);

	Page<Review> findAllByReviewerIdAndDeletedAtIsNull(Long reviewerId, Pageable pageable);

	@Query("""
		select r.productId
		from Review r
		where r.reviewerId = :reviewerId
		  and r.productId in :productIds
		  and r.auctionId is null
		  and r.deletedAt is null
		""")
	List<Long> findReviewedRegularProductIds(
		@Param("reviewerId") Long reviewerId,
		@Param("productIds") Collection<Long> productIds
	);

	@Query("""
		select r.auctionId
		from Review r
		where r.reviewerId = :reviewerId
		  and r.auctionId in :auctionIds
		  and r.deletedAt is null
		""")
	List<Long> findReviewedAuctionIds(
		@Param("reviewerId") Long reviewerId,
		@Param("auctionIds") Collection<Long> auctionIds
	);

	@Query("""
		select r
		from Review r
		where r.reviewerId in :reviewerIds
		  and r.productId in :productIds
		  and r.auctionId is null
		  and r.deletedAt is null
		""")
	List<Review> findRegularReviewsByReviewersAndProducts(
		@Param("reviewerIds") Collection<Long> reviewerIds,
		@Param("productIds") Collection<Long> productIds
	);

	@Query("""
		select r
		from Review r
		where r.reviewerId in :reviewerIds
		  and r.auctionId in :auctionIds
		  and r.deletedAt is null
		""")
	List<Review> findAuctionReviewsByReviewersAndAuctions(
		@Param("reviewerIds") Collection<Long> reviewerIds,
		@Param("auctionIds") Collection<Long> auctionIds
	);

	@Query("""
		select new com.dealit.dealit.domain.review.dto.ReviewRatingSummaryResponse(
			:revieweeId,
			coalesce(avg(r.rating), 0.0),
			count(r)
		)
		from Review r
		where r.revieweeId = :revieweeId
			and r.deletedAt is null
		""")
	ReviewRatingSummaryResponse getRatingSummaryByRevieweeId(@Param("revieweeId") Long revieweeId);
}
