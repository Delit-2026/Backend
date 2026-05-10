package com.dealit.dealit.domain.auction.repository;

import com.dealit.dealit.domain.auction.AuctionPaymentStatus;
import com.dealit.dealit.domain.auction.entity.AuctionPayment;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuctionPaymentRepository extends JpaRepository<AuctionPayment, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<AuctionPayment> findFirstByAuctionAuctionIdAndBidderIdAndStatusAndDeletedAtIsNullOrderByReservedAtDescAuctionPaymentIdDesc(
		Long auctionId,
		Long bidderId,
		AuctionPaymentStatus status
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<AuctionPayment> findByAuctionPaymentIdAndDeletedAtIsNull(Long auctionPaymentId);

	@Query("""
		SELECT p
		FROM AuctionPayment p
		WHERE p.status = :status
		  AND p.refundRequestedAt < :threshold
		  AND p.deletedAt IS NULL
		ORDER BY p.refundRequestedAt ASC, p.auctionPaymentId ASC
	""")
	List<AuctionPayment> findRefundPendingBefore(
		@Param("status") AuctionPaymentStatus status,
		@Param("threshold") OffsetDateTime threshold,
		Pageable pageable
	);
}
