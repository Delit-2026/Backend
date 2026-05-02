package com.dealit.dealit.domain.auction.scheduler;

import com.dealit.dealit.domain.auction.exception.AuctionNotFoundException;
import com.dealit.dealit.domain.auction.redis.AuctionRedisService;
import com.dealit.dealit.domain.auction.service.AuctionBidService;
import java.time.Clock;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuctionEndingScheduler {

	private final AuctionRedisService auctionRedisService;
	private final AuctionBidService auctionBidService;
	private final Clock clock;

	@Scheduled(fixedDelayString = "${app.auction.ending-scheduler-delay-ms:3000}")
	public void endExpiredAuctions() {
		Set<String> auctionIds;
		try {
			auctionIds = auctionRedisService.findEndingAuctionIds(clock.millis());
		} catch (RedisConnectionFailureException exception) {
			return;
		}
		if (auctionIds == null || auctionIds.isEmpty()) {
			return;
		}

		for (String auctionId : auctionIds) {
			Long parsedAuctionId;
			try {
				parsedAuctionId = Long.valueOf(auctionId);
			} catch (NumberFormatException exception) {
				auctionRedisService.removeEndingValue(auctionId);
				continue;
			}

			try {
				auctionBidService.endAuction(parsedAuctionId);
			} catch (AuctionNotFoundException exception) {
				// DB reset or deleted auctions can leave stale members in Redis.
			} finally {
				auctionRedisService.removeEnding(parsedAuctionId);
			}
		}
	}
}
