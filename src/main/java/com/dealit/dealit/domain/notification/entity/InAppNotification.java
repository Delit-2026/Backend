package com.dealit.dealit.domain.notification.entity;

import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "notification_seq_generator",
	sequenceName = "notification_seq",
	allocationSize = 1
)
public class InAppNotification extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_seq_generator")
	@Column(name = "notification_id")
	private Long notificationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 30)
	private InAppNotificationType type;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "content", nullable = false, length = 500)
	private String content;

	@Column(name = "target_type", length = 30)
	private String targetType;

	@Column(name = "target_id")
	private Long targetId;

	@Column(name = "target_url", length = 500)
	private String targetUrl;

	@Column(name = "read_at")
	private LocalDateTime readAt;

	private InAppNotification(
		Member member,
		InAppNotificationType type,
		String title,
		String content,
		String targetType,
		Long targetId,
		String targetUrl
	) {
		this.member = member;
		this.type = type;
		this.title = title;
		this.content = content;
		this.targetType = targetType;
		this.targetId = targetId;
		this.targetUrl = targetUrl;
	}

	public static InAppNotification create(
		Member member,
		InAppNotificationType type,
		String title,
		String content,
		String targetType,
		Long targetId,
		String targetUrl
	) {
		return new InAppNotification(member, type, title, content, targetType, targetId, targetUrl);
	}

	public boolean isRead() {
		return readAt != null;
	}

	public void markAsRead() {
		if (readAt == null) {
			readAt = LocalDateTime.now();
		}
	}
}
