package com.dealit.dealit.domain.member.entity;

import com.dealit.dealit.domain.member.LocationSource;
import com.dealit.dealit.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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

	@Column(name = "email", length = 100)
	private String email;

	@Column(name = "name", length = 30)
	private String name;

	@Column(name = "nickname", nullable = false, length = 30)
	private String nickname;

	@Column(name = "intro", length = 500)
	private String intro;

	@Column(name = "profile_image", length = 500)
	private String profileImage;

	@Column(name = "location", length = 100)
	private String location;

	@Column(name = "postal_code", length = 10)
	private String postalCode;

	@Column(name = "road_address", length = 255)
	private String roadAddress;

	@Column(name = "jibun_address", length = 255)
	private String jibunAddress;

	@Column(name = "detail_address", length = 255)
	private String detailAddress;

	@Enumerated(EnumType.STRING)
	@Column(name = "location_source", length = 20)
	private LocationSource locationSource;

	@Column(name = "is_verified", nullable = false)
	private boolean verified;

	@Builder
	private Member(
		String loginId,
		String password,
		String email,
		String name,
		String nickname,
		String intro,
		String profileImage,
		String location,
		String postalCode,
		String roadAddress,
		String jibunAddress,
		String detailAddress,
		LocationSource locationSource,
		boolean verified
	) {
		this.loginId = loginId;
		this.password = password;
		this.email = email;
		this.name = name;
		this.nickname = nickname;
		this.intro = intro;
		this.profileImage = profileImage;
		this.location = location;
		this.postalCode = postalCode;
		this.roadAddress = roadAddress;
		this.jibunAddress = jibunAddress;
		this.detailAddress = detailAddress;
		this.locationSource = locationSource;
		this.verified = verified;
	}

	public static Member create(
		String loginId,
		String encodedPassword,
		String email,
		String phoneNumber,
		String name
	) {
		return create(loginId, encodedPassword, email, name, false);
	}

	public static Member create(
		String loginId,
		String encodedPassword,
		String email,
		String name,
		boolean verified
	) {
		return Member.builder()
			.loginId(loginId)
			.password(encodedPassword)
			.email(email)
			.name(name)
			.nickname("PENDING")
			.verified(verified)
			.build();
	}

	public static Member create(
		String loginId,
		String encodedPassword,
		String email,
		String name
	) {
		return create(loginId, encodedPassword, email, name, false);
	}

	public static Member create(
		String loginId,
		String encodedPassword,
		String email,
		String phoneNumber,
		String name,
		boolean verified
	) {
		return create(loginId, encodedPassword, email, name, verified);
	}

	public void assignDefaultNickname() {
		if (memberId == null) {
			throw new IllegalStateException("memberId must be generated before assigning nickname");
		}
		this.nickname = "Dealit#" + memberId;
	}

	public void updateProfile(String name, String nickname, String intro, String profileImage) {
		this.name = name;
		this.nickname = nickname;
		this.intro = intro;
		this.profileImage = profileImage;
	}

	public void updateLocation(String location) {
		this.location = location;
		this.postalCode = null;
		this.roadAddress = null;
		this.jibunAddress = null;
		this.detailAddress = null;
		this.locationSource = LocationSource.MANUAL;
	}

	public void updateLocationDetails(
		String location,
		String postalCode,
		String roadAddress,
		String jibunAddress,
		String detailAddress,
		LocationSource locationSource
	) {
		this.location = location;
		this.postalCode = postalCode;
		this.roadAddress = roadAddress;
		this.jibunAddress = jibunAddress;
		this.detailAddress = detailAddress;
		this.locationSource = locationSource;
	}

	public void updateProfileImage(String profileImage) {
		this.profileImage = profileImage;
	}

	public void verifyEmail() {
		this.verified = true;
	}

	public void updateEmailAndVerify(String email) {
		this.email = email;
		this.verified = true;
	}
}
