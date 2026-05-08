package com.dealit.dealit.domain.auction.event;

import com.dealit.dealit.domain.auction.service.AuctionRefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionRefundEventListener {

	private final AuctionRefundService auctionRefundService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void refund(AuctionRefundRequestedEvent event) {
		try {
			auctionRefundService.refundPendingPayment(event.paymentId());
		} catch (RuntimeException exception) {
			log.warn(
				"Auction refund event handling failed. paymentId={}, auctionId={}, bidderId={}",
				event.paymentId(),
				event.auctionId(),
				event.bidderId(),
				exception
			);
		}
	}
}
