package com.dealit.dealit.domain.auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dealit.dealit.domain.auction.entity.Auction;
import com.dealit.dealit.domain.auction.service.AuctionNotificationService;
import com.dealit.dealit.domain.notification.dto.NotificationCreateRequest;
import com.dealit.dealit.domain.notification.entity.InAppNotificationType;
import com.dealit.dealit.domain.notification.service.FcmNotificationService;
import com.dealit.dealit.domain.notification.service.NotificationCenterService;
import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import com.dealit.dealit.domain.product.entity.Product;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AuctionNotificationServiceTest {

	private final NotificationCenterService notificationCenterService = mock(NotificationCenterService.class);
	private final FcmNotificationService fcmNotificationService = mock(FcmNotificationService.class);
	private final AuctionNotificationService auctionNotificationService = new AuctionNotificationService(
		notificationCenterService,
		fcmNotificationService
	);

	@Test
	@DisplayName("첫 입찰이 발생하면 판매자 알림 센터에 경매 알림을 생성한다")
	void notifyBidReceivedCreatesFirstBidNotification() {
		Auction auction = auction(10L);
		when(fcmNotificationService.sendToMember(eq(10L), any(), any(), any())).thenReturn(0);

		auctionNotificationService.notifyBidReceived(auction, 20L, new BigDecimal("101000"), 1);

		ArgumentCaptor<NotificationCreateRequest> captor = ArgumentCaptor.forClass(NotificationCreateRequest.class);
		verify(notificationCenterService).create(eq(10L), captor.capture());
		assertThat(captor.getValue().type()).isEqualTo(InAppNotificationType.AUCTION);
		assertThat(captor.getValue().title()).isEqualTo("첫 입찰 발생");
		assertThat(captor.getValue().targetType()).isEqualTo("AUCTION");
		assertThat(captor.getValue().targetUrl()).isEqualTo("/auctions/" + auction.getAuctionId());
	}

	@Test
	@DisplayName("입찰 없이 경매가 종료되면 판매자 알림 센터에 유찰 알림을 생성한다")
	void notifyAuctionEndedCreatesNoBidNotification() {
		Auction auction = auction(10L);
		when(fcmNotificationService.sendToMember(eq(10L), any(), any(), any())).thenReturn(0);

		auctionNotificationService.notifyAuctionEnded(auction, 10L, null, null, AuctionStatus.NO_BID);

		ArgumentCaptor<NotificationCreateRequest> captor = ArgumentCaptor.forClass(NotificationCreateRequest.class);
		verify(notificationCenterService).create(eq(10L), captor.capture());
		assertThat(captor.getValue().type()).isEqualTo(InAppNotificationType.AUCTION);
		assertThat(captor.getValue().title()).isEqualTo("유찰 알림");
		assertThat(captor.getValue().targetType()).isEqualTo("AUCTION");
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
		Auction auction = Auction.create(
			product,
			new BigDecimal("100000"),
			new BigDecimal("1000"),
			OffsetDateTime.now().minusDays(1),
			OffsetDateTime.now().plusDays(1),
			AuctionStatus.ONGOING
		);
		ReflectionTestUtils.setField(auction, "auctionId", 1L);
		return auction;
	}
}
