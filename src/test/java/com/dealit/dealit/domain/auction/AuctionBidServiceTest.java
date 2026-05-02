package com.dealit.dealit.domain.auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dealit.dealit.domain.auction.dto.BidResponse;
import com.dealit.dealit.domain.auction.entity.Auction;
import com.dealit.dealit.domain.auction.entity.Bid;
import com.dealit.dealit.domain.auction.event.AuctionEventPublisher;
import com.dealit.dealit.domain.auction.exception.InvalidAuctionRequestException;
import com.dealit.dealit.domain.auction.redis.AuctionRedisService;
import com.dealit.dealit.domain.auction.redis.AuctionRedisService.AuctionState;
import com.dealit.dealit.domain.auction.redis.AuctionRedisService.BidScriptResult;
import com.dealit.dealit.domain.auction.redis.AuctionRedisService.BidScriptStatus;
import com.dealit.dealit.domain.auction.repository.AuctionRepository;
import com.dealit.dealit.domain.auction.repository.BidRepository;
import com.dealit.dealit.domain.auction.repository.CategoryRepository;
import com.dealit.dealit.domain.auction.service.AuctionBidService;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.global.service.ImageUrlService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuctionBidServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-02T00:00:00Z"), ZoneOffset.UTC);

	private final AuctionRepository auctionRepository = mock(AuctionRepository.class);
	private final BidRepository bidRepository = mock(BidRepository.class);
	private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
	private final MemberRepository memberRepository = mock(MemberRepository.class);
	private final AuctionRedisService auctionRedisService = mock(AuctionRedisService.class);
	private final AuctionEventPublisher auctionEventPublisher = mock(AuctionEventPublisher.class);
	private final ImageUrlService imageUrlService = mock(ImageUrlService.class);

	private final AuctionBidService auctionBidService = new AuctionBidService(
		auctionRepository,
		bidRepository,
		categoryRepository,
		memberRepository,
		auctionRedisService,
		auctionEventPublisher,
		imageUrlService,
		FIXED_CLOCK
	);

	@Test
	@DisplayName("종료 시 입찰자가 없으면 유찰 상태로 종료한다")
	void endAuctionCompletesWithoutBidWhenNoHighestBidder() {
		Long auctionId = 1L;
		Long sellerId = 10L;
		Auction auction = auction(sellerId);
		when(auctionRepository.findByAuctionIdAndDeletedAtIsNullAndProductDeletedAtIsNull(auctionId))
			.thenReturn(Optional.of(auction));
		when(auctionRedisService.getState(auctionId))
			.thenReturn(new AuctionState(new BigDecimal("100000"), new BigDecimal("1000"), null));

		auctionBidService.endAuction(auctionId);

		assertThat(auction.getStatus()).isEqualTo(AuctionStatus.NO_BID);
		assertThat(auction.getProduct().getStatus()).isEqualTo(ProductStatus.ENDED);
		assertThat(auction.getDeletedAt()).isNotNull();
		assertThat(auction.getProduct().getDeletedAt()).isNotNull();
		assertThat(auction.getWinnerId()).isNull();
		assertThat(auction.getFinalPrice()).isNull();
		verify(auctionRedisService).removeEnding(auctionId);
		verify(auctionRedisService).deleteState(auctionId);
		verify(auctionEventPublisher).publishAuctionEnded(
			eq(sellerId),
			eq(auctionId),
			eq(null),
			eq(null),
			eq(AuctionStatus.NO_BID),
			any(OffsetDateTime.class)
		);
	}

	@Test
	@DisplayName("종료 시 최고 입찰자가 있으면 낙찰 상태와 최종가를 저장한다")
	void endAuctionCompletesWithWinnerWhenHighestBidderExists() {
		Long auctionId = 1L;
		Long sellerId = 10L;
		Long winnerId = 20L;
		BigDecimal finalPrice = new BigDecimal("150000");
		Auction auction = auction(sellerId);
		when(auctionRepository.findByAuctionIdAndDeletedAtIsNullAndProductDeletedAtIsNull(auctionId))
			.thenReturn(Optional.of(auction));
		when(auctionRedisService.getState(auctionId))
			.thenReturn(new AuctionState(finalPrice, new BigDecimal("1000"), winnerId));

		auctionBidService.endAuction(auctionId);

		assertThat(auction.getStatus()).isEqualTo(AuctionStatus.SUCCESSFUL_BID);
		assertThat(auction.getProduct().getStatus()).isEqualTo(ProductStatus.SOLD);
		assertThat(auction.getWinnerId()).isEqualTo(winnerId);
		assertThat(auction.getFinalPrice()).isEqualByComparingTo(finalPrice);
		assertThat(auction.getCurrentPrice()).isEqualByComparingTo(finalPrice);
		verify(auctionRedisService).removeEnding(auctionId);
		verify(auctionRedisService).deleteState(auctionId);
		verify(auctionEventPublisher).publishAuctionEnded(
			eq(sellerId),
			eq(auctionId),
			eq(winnerId),
			eq(finalPrice),
			eq(AuctionStatus.SUCCESSFUL_BID),
			any(OffsetDateTime.class)
		);
		verify(auctionEventPublisher).publishAuctionEnded(
			eq(winnerId),
			eq(auctionId),
			eq(winnerId),
			eq(finalPrice),
			eq(AuctionStatus.SUCCESSFUL_BID),
			any(OffsetDateTime.class)
		);
	}

	@Test
	@DisplayName("동시에 최소가를 만족한 두 입찰이 들어와도 Redis 판정 기준으로 하나만 성공한다")
	void concurrentBidsAllowOnlyOneWinnerWhenSecondBidIsTooLowAfterAtomicUpdate() throws Exception {
		Long auctionId = 1L;
		Auction auction = auction(10L);
		AtomicReference<BigDecimal> currentPrice = new AtomicReference<>(new BigDecimal("100000"));
		when(auctionRepository.findByAuctionIdAndDeletedAtIsNullAndProductDeletedAtIsNull(auctionId))
			.thenReturn(Optional.of(auction));
		when(bidRepository.save(any(Bid.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(auctionRedisService.bid(eq(auctionId), any(BigDecimal.class), anyLong()))
			.thenAnswer(invocation -> {
				BigDecimal bidPrice = invocation.getArgument(1);
				synchronized (currentPrice) {
					BigDecimal minimumNextPrice = currentPrice.get().add(new BigDecimal("1000"));
					if (bidPrice.compareTo(minimumNextPrice) < 0) {
						return new BidScriptResult(BidScriptStatus.BID_TOO_LOW, null);
					}
					currentPrice.set(bidPrice);
					return new BidScriptResult(BidScriptStatus.SUCCESS, null);
				}
			});

		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			List<Future<Object>> futures = new ArrayList<>();
			futures.add(executor.submit(() -> bidAfterStart(start, auctionId, 20L, new BigDecimal("101000"))));
			futures.add(executor.submit(() -> bidAfterStart(start, auctionId, 30L, new BigDecimal("101500"))));
			start.countDown();

			List<Object> results = List.of(futures.get(0).get(), futures.get(1).get());

			assertThat(results.stream().filter(BidResponse.class::isInstance)).hasSize(1);
			assertThat(results.stream().filter(InvalidAuctionRequestException.class::isInstance)).hasSize(1);
			assertThat(auction.getCurrentPrice()).isIn(new BigDecimal("101000"), new BigDecimal("101500"));
			verify(bidRepository, times(1)).save(any(Bid.class));
			verify(auctionEventPublisher, times(1)).publishBidUpdated(
				eq(auctionId),
				any(BigDecimal.class),
				anyLong(),
				any(OffsetDateTime.class)
			);
		}
	}

	@Test
	@DisplayName("상태가 진행중이어도 종료 시각이 지났으면 입찰을 거절한다")
	void bidFailsWhenAuctionEndTimeAlreadyPassed() {
		Long auctionId = 1L;
		Auction auction = endedByTimeAuction(10L);
		when(auctionRepository.findByAuctionIdAndDeletedAtIsNullAndProductDeletedAtIsNull(auctionId))
			.thenReturn(Optional.of(auction));

		Object result;
		try {
			result = auctionBidService.bid(auctionId, 20L, new BigDecimal("101000"));
		} catch (InvalidAuctionRequestException exception) {
			result = exception;
		}

		assertThat(result).isInstanceOf(InvalidAuctionRequestException.class);
		assertThat(((InvalidAuctionRequestException) result).getMessage()).isEqualTo("이미 종료된 경매입니다.");
		verify(auctionRedisService, never()).bid(anyLong(), any(BigDecimal.class), anyLong());
		verify(bidRepository, never()).save(any(Bid.class));
	}

	private Object bidAfterStart(CountDownLatch start, Long auctionId, Long bidderId, BigDecimal bidPrice)
		throws InterruptedException {
		start.await();
		try {
			return auctionBidService.bid(auctionId, bidderId, bidPrice);
		} catch (InvalidAuctionRequestException exception) {
			return exception;
		}
	}

	private Auction auction(Long sellerId) {
		return auction(sellerId, OffsetDateTime.now(FIXED_CLOCK).plusDays(1));
	}

	private Auction endedByTimeAuction(Long sellerId) {
		return auction(sellerId, OffsetDateTime.now(FIXED_CLOCK).minusSeconds(1));
	}

	private Auction auction(Long sellerId, OffsetDateTime endsAt) {
		Product product = Product.create(
			"auction product",
			"description",
			ProductSaleType.AUCTION,
			21L,
			sellerId,
			BigDecimal.ZERO,
			false,
			"서울 마포구",
			null,
			ProductStatus.ON_SALE
		);
		return Auction.create(
			product,
			new BigDecimal("100000"),
			new BigDecimal("1000"),
			OffsetDateTime.now(FIXED_CLOCK).minusDays(1),
			endsAt,
			AuctionStatus.ONGOING
		);
	}
}
