package com.dealit.dealit.domain.auction.service;

import com.dealit.dealit.domain.auction.AuctionPaymentStatus;
import com.dealit.dealit.domain.auction.entity.AuctionPayment;
import com.dealit.dealit.domain.auction.repository.AuctionPaymentRepository;
import com.dealit.dealit.domain.wallet.service.WalletService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionRefundService {

	private static final int REFUND_BATCH_SIZE = 100;
	private static final int TRADE_BATCH_SIZE = 100;

	private final AuctionPaymentRepository auctionPaymentRepository;
	private final WalletService walletService;
	private final Clock clock;

	@Transactional
	public void refundPendingPayment(Long paymentId) {
		AuctionPayment payment = auctionPaymentRepository.findByAuctionPaymentIdAndDeletedAtIsNull(paymentId)
			.orElse(null);
		if (payment == null || payment.getStatus() == AuctionPaymentStatus.REFUNDED) {
			return;
		}
		if (payment.getStatus() != AuctionPaymentStatus.REFUND_PENDING) {
			return;
		}

		OffsetDateTime refundedAt = OffsetDateTime.now(clock);
		if (payment.completeRefund(refundedAt)) {
			walletService.refundAuctionPayment(
				payment.getBidderId(),
				payment.getAmount(),
				payment.getAuction().getAuctionId()
			);
		}
	}

	public List<Long> findStalePendingPaymentIds() {
		OffsetDateTime threshold = OffsetDateTime.now(clock).minusSeconds(10);
		return auctionPaymentRepository.findRefundPendingBefore(
				AuctionPaymentStatus.REFUND_PENDING,
				threshold,
				PageRequest.of(0, REFUND_BATCH_SIZE)
			)
			.stream()
			.map(AuctionPayment::getAuctionPaymentId)
			.toList();
	}

	public List<Long> findUnshippedPaymentIdsPastDeadline() {
		OffsetDateTime threshold = OffsetDateTime.now(clock).minusDays(3);
		return auctionPaymentRepository.findReservedPaymentIdsBefore(
			AuctionPaymentStatus.RESERVED,
			threshold,
			PageRequest.of(0, TRADE_BATCH_SIZE)
		);
	}

	public List<Long> findShippedPaymentIdsPastReceiptDeadline() {
		OffsetDateTime threshold = OffsetDateTime.now(clock).minusDays(7);
		return auctionPaymentRepository.findShippedPaymentIdsBefore(
			AuctionPaymentStatus.SHIPPED,
			threshold,
			PageRequest.of(0, TRADE_BATCH_SIZE)
		);
	}

	@Transactional
	public void requestRefundForUnshippedPayment(Long paymentId) {
		AuctionPayment payment = auctionPaymentRepository.findByAuctionPaymentIdAndDeletedAtIsNull(paymentId)
			.orElse(null);
		if (payment == null || payment.getStatus() != AuctionPaymentStatus.RESERVED) {
			return;
		}
		payment.requestRefund(OffsetDateTime.now(clock));
	}

	@Transactional
	public void autoConfirmReceived(Long paymentId) {
		AuctionPayment payment = auctionPaymentRepository.findByAuctionPaymentIdAndDeletedAtIsNull(paymentId)
			.orElse(null);
		if (payment == null || payment.getStatus() != AuctionPaymentStatus.SHIPPED) {
			return;
		}
		if (payment.confirmReceived(OffsetDateTime.now(clock))) {
			walletService.settleAuctionPayment(
				payment.getSellerId(),
				payment.getAmount(),
				payment.getAuction().getAuctionId()
			);
		}
	}
}
