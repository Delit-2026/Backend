package com.dealit.dealit.domain.notification.controller;

import com.dealit.dealit.domain.notification.dto.DeleteFcmTokenRequest;
import com.dealit.dealit.domain.notification.dto.DeleteFcmTokenResponse;
import com.dealit.dealit.domain.notification.dto.DeleteNotificationResponse;
import com.dealit.dealit.domain.notification.dto.MarkAllNotificationsReadResponse;
import com.dealit.dealit.domain.notification.dto.MarkNotificationReadResponse;
import com.dealit.dealit.domain.notification.dto.NotificationListResponse;
import com.dealit.dealit.domain.notification.dto.RegisterFcmTokenRequest;
import com.dealit.dealit.domain.notification.dto.RegisterFcmTokenResponse;
import com.dealit.dealit.domain.notification.dto.SendTestNotificationRequest;
import com.dealit.dealit.domain.notification.dto.SendTestNotificationResponse;
import com.dealit.dealit.domain.notification.dto.UnreadNotificationCountResponse;
import com.dealit.dealit.domain.notification.service.FcmNotificationService;
import com.dealit.dealit.domain.notification.service.NotificationCenterService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

	private final FcmNotificationService fcmNotificationService;
	private final NotificationCenterService notificationCenterService;

	@Operation(summary = "내 알림 목록 조회", description = "로그인한 회원의 알림을 최신순으로 조회합니다.")
	@GetMapping
	public NotificationListResponse getMyNotifications(
		@AuthenticationPrincipal AuthenticatedMember member,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return notificationCenterService.getMyNotifications(member.memberId(), page, size);
	}

	@Operation(summary = "안 읽은 알림 개수 조회", description = "로그인한 회원의 안 읽은 알림 개수를 조회합니다.")
	@GetMapping("/unread-count")
	public UnreadNotificationCountResponse getUnreadCount(@AuthenticationPrincipal AuthenticatedMember member) {
		return notificationCenterService.getUnreadCount(member.memberId());
	}

	@Operation(summary = "개별 알림 읽음 처리", description = "로그인한 회원의 특정 알림을 읽음 처리합니다.")
	@PatchMapping("/{notificationId}/read")
	public MarkNotificationReadResponse markAsRead(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long notificationId
	) {
		return notificationCenterService.markAsRead(member.memberId(), notificationId);
	}

	@Operation(summary = "전체 알림 읽음 처리", description = "로그인한 회원의 모든 안 읽은 알림을 읽음 처리합니다.")
	@PatchMapping("/read-all")
	public MarkAllNotificationsReadResponse markAllAsRead(@AuthenticationPrincipal AuthenticatedMember member) {
		return notificationCenterService.markAllAsRead(member.memberId());
	}

	@Operation(summary = "개별 알림 삭제", description = "로그인한 회원의 특정 알림을 삭제합니다.")
	@DeleteMapping("/{notificationId}")
	public DeleteNotificationResponse deleteNotification(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long notificationId
	) {
		return notificationCenterService.delete(member.memberId(), notificationId);
	}

	@Operation(summary = "FCM 토큰 등록", description = "로그인한 회원의 브라우저/기기 FCM 토큰을 저장합니다.")
	@PostMapping("/fcm-token")
	public RegisterFcmTokenResponse registerFcmToken(
		@AuthenticationPrincipal AuthenticatedMember member,
		@Valid @RequestBody RegisterFcmTokenRequest request
	) {
		return fcmNotificationService.registerToken(member.memberId(), request);
	}

	@Operation(summary = "FCM 토큰 삭제", description = "로그아웃 또는 알림 해제 시 저장된 FCM 토큰을 삭제합니다.")
	@DeleteMapping("/fcm-token")
	public DeleteFcmTokenResponse deleteFcmToken(
		@AuthenticationPrincipal AuthenticatedMember member,
		@Valid @RequestBody DeleteFcmTokenRequest request
	) {
		return fcmNotificationService.deleteToken(member.memberId(), request.token());
	}

	@Operation(summary = "테스트 푸시 발송", description = "로그인한 회원의 등록된 FCM 토큰으로 테스트 알림을 발송합니다.")
	@PostMapping("/test")
	public SendTestNotificationResponse sendTestNotification(
		@AuthenticationPrincipal AuthenticatedMember member,
		@Valid @RequestBody SendTestNotificationRequest request
	) {
		int sentCount = fcmNotificationService.sendToMember(
			member.memberId(),
			request.title(),
			request.body(),
			request.data()
		);

		return new SendTestNotificationResponse(sentCount);
	}
}
