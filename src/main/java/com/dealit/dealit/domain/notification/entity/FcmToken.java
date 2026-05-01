package com.dealit.dealit.domain.notification.entity;

import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "fcm_token",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_fcm_token_token", columnNames = "token")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "fcm_token_seq_generator",
	sequenceName = "fcm_token_seq",
	allocationSize = 1
)
public class FcmToken extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fcm_token_seq_generator")
	@Column(name = "fcm_token_id")
	private Long fcmTokenId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "token", nullable = false, length = 4096)
	private String token;

	@Column(name = "device_id", length = 100)
	private String deviceId;

	@Column(name = "platform", length = 30)
	private String platform;

	private FcmToken(Member member, String token, String deviceId, String platform) {
		this.member = member;
		this.token = token;
		this.deviceId = deviceId;
		this.platform = platform;
	}

	public static FcmToken create(Member member, String token, String deviceId, String platform) {
		return new FcmToken(member, token, deviceId, platform);
	}

	public void update(Member member, String deviceId, String platform) {
		this.member = member;
		this.deviceId = deviceId;
		this.platform = platform;
	}
}
