package com.dealit.dealit.domain.member.service;

import com.dealit.dealit.domain.auth.exception.InvalidCredentialsException;
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

	@Transactional
	public MyPageProfileResponse updateProfile(Long memberId, UpdateMyProfileRequest request) {
		Member member = loadActiveMember(memberId);
		String nickname = request.nickname().trim();
		validateNicknameNotDuplicated(nickname, member.getMemberId());
		String bio = request.bio() != null ? normalizeBlank(request.bio()) : member.getIntro();
		String profileImage = request.profileImageUrl() != null
			? normalizeProfileImage(request.profileImageUrl())
			: member.getProfileImage();

		member.updateProfile(
			nickname,
			bio,
			profileImage
		);

		return toResponse(member);
	}

	@Transactional
	public UpdateMyLocationResponse updateLocation(Long memberId, UpdateMyLocationRequest request) {
		Member member = loadActiveMember(memberId);
		String location = request.location().trim();
		member.updateLocation(location);

		return new UpdateMyLocationResponse(location);
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
			member.getNickname(),
			member.getEmail(),
			member.getIntro(),
			toPublicProfileImageUrl(member.getProfileImage()),
			member.getLocation(),
			0.0,
			0,
			0,
			0,
			0
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
}
