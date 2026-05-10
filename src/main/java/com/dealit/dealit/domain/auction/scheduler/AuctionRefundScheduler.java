package com.dealit.dealit.domain.auction.scheduler;

import com.dealit.dealit.domain.auction.service.AuctionRefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionRefundScheduler {

	private final AuctionRefundService auctionRefundService;

	@Scheduled(fixedDelayString = "${app.auction.refund-scheduler-delay-ms:60000}")
	public void refundStalePendingPayments() {
		auctionRefundService.findStalePendingPaymentIds()
			.forEach(paymentId -> {
				try {
					auctionRefundService.refundPendingPayment(paymentId);
				} catch (RuntimeException exception) {
					log.warn("Auction refund scheduler failed. paymentId={}", paymentId, exception);
				}
			});
	}
}
