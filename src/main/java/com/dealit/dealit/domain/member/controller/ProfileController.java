package com.dealit.dealit.domain.member.controller;

import com.dealit.dealit.domain.member.dto.MyPageProfileResponse;
import com.dealit.dealit.domain.member.dto.UpdateMyLocationRequest;
import com.dealit.dealit.domain.member.dto.UpdateMyLocationResponse;
import com.dealit.dealit.domain.member.dto.UpdateMyProfileRequest;
import com.dealit.dealit.domain.member.dto.UploadProfileImageResponse;
import com.dealit.dealit.domain.member.service.ProfileService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Profile", description = "마이페이지 프로필 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class ProfileController {

	private final ProfileService profileService;

	@Operation(summary = "마이페이지 프로필 조회", description = "현재 로그인한 사용자의 마이페이지 프로필 정보를 조회합니다.")
	@GetMapping("/mypage")
	public MyPageProfileResponse getMyPageProfile(@AuthenticationPrincipal AuthenticatedMember member) {
		return profileService.getMyPageProfile(member.memberId());
	}

	@Operation(summary = "프로필 수정", description = "현재 로그인한 사용자의 닉네임, 소개글, 프로필 이미지를 수정합니다.")
	@PatchMapping("/profile")
	public MyPageProfileResponse updateProfile(
		@AuthenticationPrincipal AuthenticatedMember member,
		@Valid @RequestBody UpdateMyProfileRequest request
	) {
		return profileService.updateProfile(member.memberId(), request);
	}

	@Operation(summary = "지역 수정", description = "현재 로그인한 사용자의 지역 정보를 수정합니다.")
	@PatchMapping("/location")
	public UpdateMyLocationResponse updateLocation(
		@AuthenticationPrincipal AuthenticatedMember member,
		@Valid @RequestBody UpdateMyLocationRequest request
	) {
		return profileService.updateLocation(member.memberId(), request);
	}

	@Operation(summary = "프로필 이미지 업로드", description = "현재 로그인한 사용자의 프로필 이미지를 업로드합니다.")
	@PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public UploadProfileImageResponse uploadProfileImage(
		@AuthenticationPrincipal AuthenticatedMember member,
		@RequestPart("file") MultipartFile file
	) {
		return profileService.uploadProfileImage(member.memberId(), file);
	}
}
