package com.dealit.dealit.domain.auction.scheduler;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.entity.Auction;
import com.dealit.dealit.domain.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class ReauctionExpirationScheduler {

	private final AuctionRepository auctionRepository;
	private final Clock clock;

	@Transactional
	@Scheduled(fixedDelayString = "${app.auction.reauction-expiration-scheduler-delay-ms:3600000}")
	public void expireReauctionCandidates() {
		OffsetDateTime now = OffsetDateTime.now(clock);
		for (Auction auction : auctionRepository.findAllByStatusAndReauctionExpiresAtLessThanEqualAndDeletedAtIsNullAndProductDeletedAtIsNull(
			AuctionStatus.NO_BID,
			now
		)) {
			auction.expireReauction();
		}
	}
}
