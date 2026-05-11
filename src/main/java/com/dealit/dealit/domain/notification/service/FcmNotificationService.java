package com.dealit.dealit.domain.notification.service;

import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.notification.dto.DeleteFcmTokenResponse;
import com.dealit.dealit.domain.notification.dto.RegisterFcmTokenRequest;
import com.dealit.dealit.domain.notification.dto.RegisterFcmTokenResponse;
import com.dealit.dealit.domain.notification.entity.FcmToken;
import com.dealit.dealit.domain.notification.exception.NotificationException;
import com.dealit.dealit.domain.notification.repository.FcmTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FcmNotificationService {

	private final FcmTokenRepository fcmTokenRepository;
	private final MemberRepository memberRepository;
	private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

	@Transactional
	public RegisterFcmTokenResponse registerToken(Long memberId, RegisterFcmTokenRequest request) {
		Member member = memberRepository.findByMemberIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new NotificationException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."));

		String token = request.token().trim();
		FcmToken fcmToken = fcmTokenRepository.findByToken(token)
			.map(existingToken -> {
				existingToken.update(member, normalizeBlank(request.deviceId()), normalizeBlank(request.platform()));
				existingToken.restore();
				return existingToken;
			})
			.orElseGet(() -> FcmToken.create(
				member,
				token,
				normalizeBlank(request.deviceId()),
				normalizeBlank(request.platform())
			));

		FcmToken savedToken = fcmTokenRepository.save(fcmToken);

		return new RegisterFcmTokenResponse(savedToken.getFcmTokenId(), true);
	}

	@Transactional
	public DeleteFcmTokenResponse deleteToken(Long memberId, String token) {
		return fcmTokenRepository.findByMemberMemberIdAndTokenAndDeletedAtIsNull(memberId, token.trim())
			.map(fcmToken -> {
				fcmToken.softDelete();
				return new DeleteFcmTokenResponse(true);
			})
			.orElseGet(() -> new DeleteFcmTokenResponse(false));
	}

	@Transactional(noRollbackFor = NotificationException.class)
	public int sendToMember(Long memberId, String title, String body, Map<String, String> data) {
		FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();

		if (firebaseMessaging == null) {
			throw new NotificationException(
				HttpStatus.SERVICE_UNAVAILABLE,
				"FIREBASE_DISABLED",
				"Firebase가 비활성화되어 있습니다. FIREBASE_ENABLED와 서비스 계정 설정을 확인하세요."
			);
		}

		int sentCount = 0;
		for (FcmToken fcmToken : fcmTokenRepository.findAllByMemberMemberIdAndDeletedAtIsNull(memberId)) {
			if (send(firebaseMessaging, fcmToken, title, body, data)) {
				sentCount++;
			}
		}

		return sentCount;
	}

	private boolean send(
		FirebaseMessaging firebaseMessaging,
		FcmToken fcmToken,
		String title,
		String body,
		Map<String, String> data
	) {
		Message.Builder messageBuilder = Message.builder()
			.setToken(fcmToken.getToken())
			.setNotification(Notification.builder()
				.setTitle(title)
				.setBody(body)
				.build());

		if (data != null && !data.isEmpty()) {
			messageBuilder.putAllData(data);
		}

		try {
			firebaseMessaging.send(messageBuilder.build());
			return true;
		} catch (FirebaseMessagingException exception) {
			if (exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
				fcmToken.softDelete();
			}
			return false;
		}
	}

	private String normalizeBlank(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
