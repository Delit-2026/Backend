package com.dealit.dealit.domain.notification.service;

import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.notification.dto.DeleteNotificationResponse;
import com.dealit.dealit.domain.notification.dto.MarkAllNotificationsReadResponse;
import com.dealit.dealit.domain.notification.dto.MarkNotificationReadResponse;
import com.dealit.dealit.domain.notification.dto.NotificationCreateRequest;
import com.dealit.dealit.domain.notification.dto.NotificationListResponse;
import com.dealit.dealit.domain.notification.dto.NotificationResponse;
import com.dealit.dealit.domain.notification.dto.UnreadNotificationCountResponse;
import com.dealit.dealit.domain.notification.entity.InAppNotification;
import com.dealit.dealit.domain.notification.exception.NotificationException;
import com.dealit.dealit.domain.notification.repository.InAppNotificationRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationCenterService {

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

	private final InAppNotificationRepository notificationRepository;
	private final MemberRepository memberRepository;

	public NotificationListResponse getMyNotifications(Long memberId, int page, int size) {
		validateActiveMember(memberId);
		int normalizedPage = Math.max(page, 0);
		int normalizedSize = Math.min(Math.max(size, 1), 100);

		Page<InAppNotification> notificationPage = notificationRepository.findAllByMemberMemberIdAndDeletedAtIsNull(
			memberId,
			PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"))
		);

		return new NotificationListResponse(
			notificationPage.getContent().stream()
				.map(this::toResponse)
				.toList(),
			notificationPage.getNumber(),
			notificationPage.getSize(),
			notificationPage.getTotalElements(),
			notificationPage.hasNext()
		);
	}

	public UnreadNotificationCountResponse getUnreadCount(Long memberId) {
		validateActiveMember(memberId);
		return new UnreadNotificationCountResponse(
			notificationRepository.countByMemberMemberIdAndReadAtIsNullAndDeletedAtIsNull(memberId)
		);
	}

	@Transactional
	public MarkNotificationReadResponse markAsRead(Long memberId, Long notificationId) {
		validateActiveMember(memberId);
		InAppNotification notification = notificationRepository
			.findByNotificationIdAndMemberMemberIdAndDeletedAtIsNull(notificationId, memberId)
			.orElseThrow(() -> new NotificationException(
				HttpStatus.NOT_FOUND,
				"NOTIFICATION_NOT_FOUND",
				"알림을 찾을 수 없습니다."
			));

		notification.markAsRead();
		return new MarkNotificationReadResponse(notification.getNotificationId(), notification.isRead());
	}

	@Transactional
	public MarkAllNotificationsReadResponse markAllAsRead(Long memberId) {
		validateActiveMember(memberId);
		int updatedCount = notificationRepository.markAllAsReadByMemberId(memberId, LocalDateTime.now());
		return new MarkAllNotificationsReadResponse(updatedCount);
	}

	@Transactional
	public DeleteNotificationResponse delete(Long memberId, Long notificationId) {
		validateActiveMember(memberId);
		InAppNotification notification = notificationRepository
			.findByNotificationIdAndMemberMemberIdAndDeletedAtIsNull(notificationId, memberId)
			.orElseThrow(() -> new NotificationException(
				HttpStatus.NOT_FOUND,
				"NOTIFICATION_NOT_FOUND",
				"알림을 찾을 수 없습니다."
			));

		notification.softDelete();
		return new DeleteNotificationResponse(notification.getNotificationId(), true);
	}

	@Transactional
	public NotificationResponse create(Long receiverMemberId, NotificationCreateRequest request) {
		Member member = findActiveMember(receiverMemberId);
		InAppNotification notification = notificationRepository.save(InAppNotification.create(
			member,
			request.type(),
			request.title().trim(),
			request.content().trim(),
			normalizeBlank(request.targetType()),
			request.targetId(),
			normalizeBlank(request.targetUrl())
		));

		return toResponse(notification);
	}

	private void validateActiveMember(Long memberId) {
		findActiveMember(memberId);
	}

	private Member findActiveMember(Long memberId) {
		return memberRepository.findByMemberIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new NotificationException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."));
	}

	private NotificationResponse toResponse(InAppNotification notification) {
		return new NotificationResponse(
			notification.getNotificationId(),
			notification.getType(),
			notification.getTitle(),
			notification.getContent(),
			notification.isRead(),
			notification.getTargetType(),
			notification.getTargetId(),
			notification.getTargetUrl(),
			toSeoulOffsetDateTime(notification.getCreatedAt()),
			toSeoulOffsetDateTime(notification.getReadAt())
		);
	}

	private OffsetDateTime toSeoulOffsetDateTime(LocalDateTime dateTime) {
		if (dateTime == null) {
			return null;
		}
		return dateTime.atZone(SEOUL_ZONE).toOffsetDateTime();
	}

	private String normalizeBlank(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
