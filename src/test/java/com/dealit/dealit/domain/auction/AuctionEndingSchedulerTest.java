package com.dealit.dealit.domain.auction;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dealit.dealit.domain.auction.exception.AuctionNotFoundException;
import com.dealit.dealit.domain.auction.redis.AuctionRedisService;
import com.dealit.dealit.domain.auction.scheduler.AuctionEndingScheduler;
import com.dealit.dealit.domain.auction.service.AuctionBidService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuctionEndingSchedulerTest {

	private final AuctionRedisService auctionRedisService = mock(AuctionRedisService.class);
	private final AuctionBidService auctionBidService = mock(AuctionBidService.class);
	private final Clock clock = Clock.fixed(Instant.parse("2026-05-02T00:00:00Z"), ZoneOffset.UTC);
	private final AuctionEndingScheduler scheduler = new AuctionEndingScheduler(
		auctionRedisService,
		auctionBidService,
		clock
	);

	@Test
	@DisplayName("종료 큐에 DB에 없는 경매 ID가 남아 있어도 제거하고 다음 실행을 막지 않는다")
	void endExpiredAuctionsRemovesStaleAuctionIdWhenAuctionDoesNotExist() {
		when(auctionRedisService.findEndingAuctionIds(clock.millis())).thenReturn(Set.of("999"));
		doThrow(new AuctionNotFoundException("존재하지 않는 경매입니다."))
			.when(auctionBidService)
			.endAuction(999L);

		scheduler.endExpiredAuctions();

		verify(auctionBidService).endAuction(999L);
		verify(auctionRedisService).removeEnding(999L);
	}

	@Test
	@DisplayName("종료 큐에 숫자가 아닌 값이 있어도 제거한다")
	void endExpiredAuctionsRemovesInvalidAuctionIdValue() {
		when(auctionRedisService.findEndingAuctionIds(clock.millis())).thenReturn(Set.of("bad-id"));

		scheduler.endExpiredAuctions();

		verify(auctionRedisService).removeEndingValue("bad-id");
	}
}
