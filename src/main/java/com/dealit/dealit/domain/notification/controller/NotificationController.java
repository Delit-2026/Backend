package com.dealit.dealit.domain.notification.controller;

import com.dealit.dealit.domain.notification.dto.DeleteFcmTokenRequest;
import com.dealit.dealit.domain.notification.dto.DeleteFcmTokenResponse;
import com.dealit.dealit.domain.notification.dto.RegisterFcmTokenRequest;
import com.dealit.dealit.domain.notification.dto.RegisterFcmTokenResponse;
import com.dealit.dealit.domain.notification.dto.SendTestNotificationRequest;
import com.dealit.dealit.domain.notification.dto.SendTestNotificationResponse;
import com.dealit.dealit.domain.notification.service.FcmNotificationService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

	private final FcmNotificationService fcmNotificationService;

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
