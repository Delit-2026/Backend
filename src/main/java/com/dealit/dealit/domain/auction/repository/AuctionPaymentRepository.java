package com.dealit.dealit.domain.auction.repository;

import com.dealit.dealit.domain.auction.AuctionPaymentStatus;
import com.dealit.dealit.domain.auction.entity.AuctionPayment;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface AuctionPaymentRepository extends JpaRepository<AuctionPayment, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<AuctionPayment> findFirstByAuctionAuctionIdAndBidderIdAndStatusAndDeletedAtIsNullOrderByReservedAtDescAuctionPaymentIdDesc(
		Long auctionId,
		Long bidderId,
		AuctionPaymentStatus status
	);
}
