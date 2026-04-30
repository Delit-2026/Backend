package com.dealit.dealit.domain.member.service;

import com.dealit.dealit.domain.auth.exception.InvalidCredentialsException;
import com.dealit.dealit.domain.member.LocationSource;
import com.dealit.dealit.domain.member.dto.LocationDetails;
import com.dealit.dealit.domain.member.dto.MyLocationResponse;
import com.dealit.dealit.domain.member.dto.MyPageProfileResponse;
import com.dealit.dealit.domain.member.dto.UpdateMyLocationRequest;
import com.dealit.dealit.domain.member.dto.UpdateMyLocationResponse;
import com.dealit.dealit.domain.member.dto.UpdateMyProfileRequest;
import com.dealit.dealit.domain.member.dto.UploadProfileImageResponse;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.exception.DuplicateNicknameException;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.global.service.ImageUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfileService {

	private final MemberRepository memberRepository;
	private final ProfileImageStorage profileImageStorage;
	private final ImageUrlService imageUrlService;

	@Transactional(readOnly = true)
	public MyPageProfileResponse getMyPageProfile(Long memberId) {
		return toResponse(loadActiveMember(memberId));
	}

	@Transactional(readOnly = true)
	public MyLocationResponse getMyLocation(Long memberId) {
		Member member = loadActiveMember(memberId);
		LocationDetails locationDetails = toLocationDetails(member);
		return new MyLocationResponse(
			locationDetails.location(),
			locationDetails.postalCode(),
			locationDetails.roadAddress(),
			locationDetails.jibunAddress(),
			locationDetails.detailAddress(),
			locationDetails.latitude(),
			locationDetails.longitude(),
			locationDetails.locationSource()
		);
	}

	@Transactional
	public MyPageProfileResponse updateProfile(Long memberId, UpdateMyProfileRequest request) {
		Member member = loadActiveMember(memberId);
		String nickname = request.nickname().trim();
		validateNicknameNotDuplicated(nickname, member.getMemberId());
		String name = request.name() != null ? normalizeBlank(request.name()) : member.getName();
		String bio = request.bio() != null ? normalizeBlank(request.bio()) : member.getIntro();
		String profileImage = request.profileImageUrl() != null
			? normalizeProfileImage(request.profileImageUrl())
			: member.getProfileImage();

		member.updateProfile(name, nickname, bio, profileImage);
		return toResponse(member);
	}

	@Transactional
	public UpdateMyLocationResponse updateLocation(Long memberId, UpdateMyLocationRequest request) {
		Member member = loadActiveMember(memberId);
		String roadAddress = normalizeBlank(request.roadAddress());
		String jibunAddress = normalizeBlank(request.jibunAddress());
		String detailAddress = normalizeBlank(request.detailAddress());
		String postalCode = normalizeBlank(request.postalCode());
		String location = resolveDisplayLocation(request.location(), roadAddress, jibunAddress, detailAddress);
		LocationSource locationSource = resolveLocationSource(request.locationSource(), roadAddress, jibunAddress);

		member.updateLocationDetails(
			location,
			postalCode,
			roadAddress,
			jibunAddress,
			detailAddress,
			request.latitude(),
			request.longitude(),
			locationSource
		);

		return new UpdateMyLocationResponse(
			location,
			postalCode,
			roadAddress,
			jibunAddress,
			detailAddress,
			request.latitude(),
			request.longitude(),
			locationSource
		);
	}

	@Transactional
	public UploadProfileImageResponse uploadProfileImage(Long memberId, MultipartFile file) {
		Member member = loadActiveMember(memberId);
		String storedFileName = profileImageStorage.store(member.getMemberId(), file);
		String profileImagePath = imageUrlService.toProfileImagePath(storedFileName);
		member.updateProfileImage(profileImagePath);

		return new UploadProfileImageResponse(imageUrlService.toPublicUrl(profileImagePath));
	}

	private Member loadActiveMember(Long memberId) {
		return memberRepository.findByMemberIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new InvalidCredentialsException("존재하지 않는 회원입니다."));
	}

	private void validateNicknameNotDuplicated(String nickname, Long memberId) {
		if (memberRepository.existsByNicknameAndMemberIdNot(nickname, memberId)) {
			throw new DuplicateNicknameException("이미 사용 중인 닉네임입니다.");
		}
	}

	private MyPageProfileResponse toResponse(Member member) {
		return new MyPageProfileResponse(
			member.getMemberId(),
			member.getName(),
			member.getNickname(),
			member.getEmail(),
			member.getIntro(),
			toPublicProfileImageUrl(member.getProfileImage()),
			member.getLocation(),
			member.isVerified(),
			0.0,
			0,
			0,
			0,
			0
		);
	}

	private LocationDetails toLocationDetails(Member member) {
		return new LocationDetails(
			member.getLocation(),
			member.getPostalCode(),
			member.getRoadAddress(),
			member.getJibunAddress(),
			member.getDetailAddress(),
			member.getLatitude(),
			member.getLongitude(),
			member.getLocationSource()
		);
	}

	private String toPublicProfileImageUrl(String profileImage) {
		if (profileImage == null || profileImage.isBlank()) {
			return null;
		}

		return imageUrlService.toPublicUrl(profileImage);
	}

	private String normalizeProfileImage(String profileImageUrl) {
		String normalized = normalizeBlank(profileImageUrl);
		if (normalized == null) {
			return null;
		}

		return imageUrlService.toStoragePath(normalized);
	}

	private String normalizeBlank(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String resolveDisplayLocation(
		String requestLocation,
		String roadAddress,
		String jibunAddress,
		String detailAddress
	) {
		String baseAddress = roadAddress != null ? roadAddress : jibunAddress;
		if (baseAddress == null) {
			return normalizeBlank(requestLocation);
		}

		return detailAddress == null ? baseAddress : baseAddress + " " + detailAddress;
	}

	private LocationSource resolveLocationSource(
		LocationSource requestedSource,
		String roadAddress,
		String jibunAddress
	) {
		if (requestedSource != null) {
			return requestedSource;
		}

		if (roadAddress != null || jibunAddress != null) {
			return LocationSource.POSTCODE;
		}

		return LocationSource.MANUAL;
	}
}
