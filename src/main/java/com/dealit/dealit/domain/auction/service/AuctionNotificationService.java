package com.dealit.dealit.domain.auction.service;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.entity.Auction;
import com.dealit.dealit.domain.notification.dto.NotificationCreateRequest;
import com.dealit.dealit.domain.notification.entity.InAppNotificationType;
import com.dealit.dealit.domain.notification.service.FcmNotificationService;
import com.dealit.dealit.domain.notification.service.NotificationCenterService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionNotificationService {

	private static final String TARGET_TYPE = "AUCTION";

	private final NotificationCenterService notificationCenterService;
	private final FcmNotificationService fcmNotificationService;

	public void notifyBidReceived(Auction auction, Long bidderId, BigDecimal bidPrice, long bidCount) {
		Long sellerId = auction.getProduct().getMemberId();
		String title = bidCount <= 1 ? "첫 입찰 발생" : "새로운 입찰가 알림";
		String content = "'" + auction.getProduct().getName() + "' 경매에 " + formatPrice(bidPrice) + "원 입찰이 들어왔어요.";
		createAuctionNotification(sellerId, title, content, auction.getAuctionId());
		sendAuctionPush(sellerId, title, content, "AUCTION_BID_RECEIVED", auction.getAuctionId(), bidderId);
	}

	public void notifyBidPlaced(Auction auction, Long bidderId, BigDecimal bidPrice) {
		String title = "새로운 입찰가 알림";
		String content = "'" + auction.getProduct().getName() + "' 경매에 " + formatPrice(bidPrice) + "원 입찰이 등록되었어요.";
		createAuctionNotification(bidderId, title, content, auction.getAuctionId());
		sendAuctionPush(bidderId, title, content, "AUCTION_BID_PLACED", auction.getAuctionId(), bidderId);
	}

	public void notifyOutbid(Auction auction, Long previousBidderId, Long newBidderId, BigDecimal bidPrice) {
		if (previousBidderId == null || previousBidderId.equals(newBidderId)) {
			return;
		}

		String title = "입찰 추월 알림";
		String content = "'" + auction.getProduct().getName() + "' 경매에서 다른 입찰자가 " + formatPrice(bidPrice) + "원으로 입찰했어요.";
		createAuctionNotification(previousBidderId, title, content, auction.getAuctionId());
		sendAuctionPush(previousBidderId, title, content, "AUCTION_OUTBID", auction.getAuctionId(), newBidderId);
	}

	public void notifyClosingSoon(Auction auction, Collection<Long> bidderIds) {
		String title = "경매 마감 임박";
		String content = "'" + auction.getProduct().getName() + "' 경매가 10분 후 마감되어요!";
		for (Long bidderId : bidderIds) {
			createAuctionNotification(bidderId, title, content, auction.getAuctionId());
			sendAuctionPush(bidderId, title, content, "AUCTION_CLOSING_SOON", auction.getAuctionId(), null);
		}
	}

	public void notifyAuctionEnded(Auction auction, Long receiverId, Long winnerId, BigDecimal finalPrice, AuctionStatus status) {
		if (receiverId == null) {
			return;
		}

		if (status == AuctionStatus.NO_BID) {
			String title = "경매가 유찰 되었어요";
			String content = "'" + auction.getProduct().getName() + "' 경매가 입찰 없이 종료되었어요. 재등록 해볼까요?";
			createAuctionNotification(receiverId, title, content, auction.getAuctionId());
			sendAuctionPush(receiverId, title, content, "AUCTION_NO_BID", auction.getAuctionId(), null);
			return;
		}

		if (status == AuctionStatus.SUCCESSFUL_BID) {
			String title = receiverId.equals(winnerId) ? "낙찰 성공 알림" : "최종 낙찰 알림";
			String content = "'" + auction.getProduct().getName() + "' 경매가 " + formatPrice(finalPrice) + "원에 낙찰되었어요.";
			createAuctionNotification(receiverId, title, content, auction.getAuctionId());
			sendAuctionPush(receiverId, title, content, "AUCTION_SUCCESSFUL_BID", auction.getAuctionId(), winnerId);
		}
	}

	public void notifyAuctionFailed(Auction auction, Long bidderId, Long winnerId, BigDecimal finalPrice) {
		String title = "낙찰 실패 알림";
		String content = "'" + auction.getProduct().getName() + "' 경매가 " + formatPrice(finalPrice) + "원에 마감되어 낙찰되지 않았어요.";
		createAuctionNotification(bidderId, title, content, auction.getAuctionId());
		sendAuctionPush(bidderId, title, content, "AUCTION_BID_FAILED", auction.getAuctionId(), winnerId);
	}

	public void notifyAuctionShipped(Auction auction, Long winnerId, Long roomId) {
		String title = "상품이 발송되었습니다.";
		String content = "'" + auction.getProduct().getName() + "' 상품이 발송되었어요. 물건을 받으면 수령확정을 눌러주세요.";
		String targetUrl = chatTargetUrl(roomId, auction.getAuctionId());
		createChatNotification(winnerId, title, content, auction.getAuctionId(), roomId, targetUrl);
		sendAuctionPush(winnerId, title, content, "AUCTION_SHIPPED", auction.getAuctionId(), null, targetUrl, roomId);
	}

	public void notifyAuctionReceived(Auction auction, Long sellerId, Long roomId) {
		String title = "수령확정이 완료되었습니다.";
		String content = "'" + auction.getProduct().getName() + "' 상품 구매자가 수령확정을 완료했어요.";
		String targetUrl = chatTargetUrl(roomId, auction.getAuctionId());
		createChatNotification(sellerId, title, content, auction.getAuctionId(), roomId, targetUrl);
		sendAuctionPush(sellerId, title, content, "AUCTION_RECEIVED", auction.getAuctionId(), null, targetUrl, roomId);
	}

	public void notifyReviewRequest(Auction auction, Long winnerId) {
		String title = "후기 작성 알림";
		String content = "'" + auction.getProduct().getName() + "' 거래 후기를 작성해 주세요.";
		String targetUrl = reviewTargetUrl(auction);
		createAuctionNotification(winnerId, title, content, auction.getAuctionId(), targetUrl);
		sendAuctionPush(winnerId, title, content, "REVIEW_REQUESTED", auction.getAuctionId(), null, targetUrl);
	}

	private void createAuctionNotification(Long receiverId, String title, String content, Long auctionId) {
		createAuctionNotification(receiverId, title, content, auctionId, "/auctions/" + auctionId);
	}

	private void createAuctionNotification(Long receiverId, String title, String content, Long auctionId, String targetUrl) {
		notificationCenterService.create(
			receiverId,
			new NotificationCreateRequest(
				InAppNotificationType.AUCTION,
				title,
				content,
				TARGET_TYPE,
				auctionId,
				targetUrl
			)
		);
	}

	private void createChatNotification(
		Long receiverId,
		String title,
		String content,
		Long auctionId,
		Long roomId,
		String targetUrl
	) {
		notificationCenterService.create(
			receiverId,
			new NotificationCreateRequest(
				InAppNotificationType.AUCTION,
				title,
				content,
				roomId == null ? TARGET_TYPE : "CHAT",
				roomId == null ? auctionId : roomId,
				targetUrl
			)
		);
	}

	private void sendAuctionPush(
		Long receiverId,
		String title,
		String content,
		String type,
		Long auctionId,
		Long actorId
	) {
		sendAuctionPush(receiverId, title, content, type, auctionId, actorId, "/auctions/" + auctionId);
	}

	private void sendAuctionPush(
		Long receiverId,
		String title,
		String content,
		String type,
		Long auctionId,
		Long actorId,
		String targetUrl
	) {
		sendAuctionPush(receiverId, title, content, type, auctionId, actorId, targetUrl, null);
	}

	private void sendAuctionPush(
		Long receiverId,
		String title,
		String content,
		String type,
		Long auctionId,
		Long actorId,
		String targetUrl,
		Long roomId
	) {
		try {
			Map<String, String> data = actorId == null
				? auctionPushData(type, auctionId, null, targetUrl, roomId)
				: auctionPushData(type, auctionId, actorId, targetUrl, roomId);
			int sentCount = fcmNotificationService.sendToMember(receiverId, title, content, data);
			log.debug("Sent auction push notification. type={}, auctionId={}, receiverId={}, sentCount={}",
				type, auctionId, receiverId, sentCount);
		} catch (RuntimeException exception) {
			log.warn("Failed to send auction push notification. type={}, auctionId={}, receiverId={}",
				type, auctionId, receiverId, exception);
		}
	}

	private String formatPrice(BigDecimal price) {
		if (price == null) {
			return "";
		}
		return NumberFormat.getNumberInstance(Locale.KOREA).format(price.stripTrailingZeros());
	}

	private String reviewTargetUrl(Auction auction) {
		return "/mypage/review/write?auctionId=" + auction.getAuctionId()
			+ "&displayProductId=" + auction.getProduct().getProductId();
	}

	private String chatTargetUrl(Long roomId, Long auctionId) {
		return roomId == null ? "/auctions/" + auctionId : "/chats/" + roomId;
	}

	private Map<String, String> auctionPushData(
		String type,
		Long auctionId,
		Long actorId,
		String targetUrl,
		Long roomId
	) {
		if (actorId != null && roomId != null) {
			return Map.of(
				"type", type,
				"auctionId", String.valueOf(auctionId),
				"actorId", String.valueOf(actorId),
				"targetUrl", targetUrl,
				"roomId", String.valueOf(roomId)
			);
		}
		if (actorId != null) {
			return Map.of(
				"type", type,
				"auctionId", String.valueOf(auctionId),
				"actorId", String.valueOf(actorId),
				"targetUrl", targetUrl
			);
		}
		if (roomId != null) {
			return Map.of(
				"type", type,
				"auctionId", String.valueOf(auctionId),
				"targetUrl", targetUrl,
				"roomId", String.valueOf(roomId)
			);
		}
		return Map.of(
			"type", type,
			"auctionId", String.valueOf(auctionId),
			"targetUrl", targetUrl
		);
	}
}
