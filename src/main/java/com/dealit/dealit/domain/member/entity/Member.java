package com.dealit.dealit.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
	name = "member",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_member_login_id", columnNames = "login_id"),
		@UniqueConstraint(name = "uk_member_email", columnNames = "email")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "member_id")
	private Long memberId;

	@Column(name = "login_id", nullable = false, length = 30)
	private String loginId;

	@Column(name = "password", nullable = false, length = 255)
	private String password;

	@Column(name = "email", nullable = false, length = 100)
	private String email;

	@Column(name = "phone_number", length = 15)
	private String phoneNumber;

	@Column(name = "name", length = 30)
	private String name;

	@Column(name = "nickname", nullable = false, length = 30)
	private String nickname;

	@Column(name = "intro", length = 500)
	private String intro;

	@Column(name = "profile_image", length = 255)
	private String profileImage;

	@Column(name = "is_verified", nullable = false)
	private boolean verified;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Builder
	private Member(
		String loginId,
		String password,
		String email,
		String phoneNumber,
		String name,
		String nickname,
		String intro,
		String profileImage,
		boolean verified,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		LocalDateTime deletedAt
	) {
		this.loginId = loginId;
		this.password = password;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.name = name;
		this.nickname = nickname;
		this.intro = intro;
		this.profileImage = profileImage;
		this.verified = verified;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.deletedAt = deletedAt;
	}

	public static Member create(
		String loginId,
		String encodedPassword,
		String email,
		String phoneNumber,
		String name
	) {
		LocalDateTime now = LocalDateTime.now();
		return Member.builder()
			.loginId(loginId)
			.password(encodedPassword)
			.email(email)
			.phoneNumber(phoneNumber)
			.name(name)
			.nickname("PENDING")
			.verified(false)
			.createdAt(now)
			.updatedAt(now)
			.build();
	}

	public void assignDefaultNickname() {
		if (memberId == null) {
			throw new IllegalStateException("memberId must be generated before assigning nickname");
		}
		this.nickname = "Dealit#" + memberId;
		this.updatedAt = LocalDateTime.now();
	}
}
