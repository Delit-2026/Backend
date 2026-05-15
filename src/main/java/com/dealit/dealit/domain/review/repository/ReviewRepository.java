package com.dealit.dealit.domain.review.repository;

import com.dealit.dealit.domain.review.dto.ReviewRatingSummaryResponse;
import com.dealit.dealit.domain.review.entity.Review;
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
