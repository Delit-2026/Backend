package com.dealit.dealit.domain.member.entity;

import com.dealit.dealit.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
@SequenceGenerator(
		name = "member_seq_generator",
		sequenceName = "member_seq",
		allocationSize = 1
)
public class Member extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq_generator")
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
		boolean verified
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
	}

	public static Member create(
		String loginId,
		String encodedPassword,
		String email,
		String phoneNumber,
		String name
	) {
		return Member.builder()
			.loginId(loginId)
			.password(encodedPassword)
			.email(email)
			.phoneNumber(phoneNumber)
			.name(name)
			.nickname("PENDING")
			.verified(false)
			.build();
	}

	public void assignDefaultNickname() {
		if (memberId == null) {
			throw new IllegalStateException("memberId must be generated before assigning nickname");
		}
		this.nickname = "Dealit#" + memberId;
	}
}
