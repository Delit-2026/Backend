package com.dealit.dealit.domain.auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dealit.dealit.domain.auction.entity.Auction;
import com.dealit.dealit.domain.auction.entity.AuctionPayment;
import com.dealit.dealit.domain.auction.repository.AuctionPaymentRepository;
import com.dealit.dealit.domain.auction.service.AuctionRefundService;
import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuctionRefundServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-02T00:00:00Z"), ZoneOffset.UTC);

	private final AuctionPaymentRepository auctionPaymentRepository = mock(AuctionPaymentRepository.class);
	private final WalletService walletService = mock(WalletService.class);
	private final AuctionRefundService auctionRefundService = new AuctionRefundService(
		auctionPaymentRepository,
		walletService,
		FIXED_CLOCK
	);

	@Test
	@DisplayName("환불 대기 결제는 지갑 환불 후 환불 완료 상태로 변경한다")
	void refundPendingPaymentCompletesRefund() {
		Long paymentId = 1L;
		Long bidderId = 20L;
		Auction auction = auction(10L);
		AuctionPayment payment = AuctionPayment.reserve(
			auction,
			bidderId,
			10L,
			101000L,
			OffsetDateTime.now(FIXED_CLOCK).minusMinutes(1)
		);
		payment.requestRefund(OffsetDateTime.now(FIXED_CLOCK).minusSeconds(30));
		when(auctionPaymentRepository.findByAuctionPaymentIdAndDeletedAtIsNull(paymentId))
			.thenReturn(Optional.of(payment));

		auctionRefundService.refundPendingPayment(paymentId);

		assertThat(payment.getStatus()).isEqualTo(AuctionPaymentStatus.REFUNDED);
		assertThat(payment.getRefundedAt()).isEqualTo(OffsetDateTime.now(FIXED_CLOCK));
		verify(walletService).refundAuctionPayment(bidderId, 101000L, auction.getAuctionId());
	}

	@Test
	@DisplayName("이미 환불 완료된 결제는 중복 환불하지 않는다")
	void refundPendingPaymentIsIdempotentWhenAlreadyRefunded() {
		Long paymentId = 1L;
		AuctionPayment payment = AuctionPayment.reserve(
			auction(10L),
			20L,
			10L,
			101000L,
			OffsetDateTime.now(FIXED_CLOCK).minusMinutes(1)
		);
		payment.requestRefund(OffsetDateTime.now(FIXED_CLOCK).minusSeconds(30));
		payment.completeRefund(OffsetDateTime.now(FIXED_CLOCK).minusSeconds(10));
		when(auctionPaymentRepository.findByAuctionPaymentIdAndDeletedAtIsNull(paymentId))
			.thenReturn(Optional.of(payment));

		auctionRefundService.refundPendingPayment(paymentId);

		verify(walletService, never()).refundAuctionPayment(anyLong(), anyLong(), anyLong());
	}

	private Auction auction(Long sellerId) {
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
			OffsetDateTime.now(FIXED_CLOCK).plusDays(1),
			AuctionStatus.ONGOING
		);
	}
}
