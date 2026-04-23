package com.dealit.dealit.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
	@NotBlank(message = "닉네임은 필수입니다.")
	@Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하이어야 합니다.")
	String nickname,

	@Size(max = 500, message = "소개글은 500자 이하이어야 합니다.")
	String bio,

	@Size(max = 500, message = "프로필 이미지 URL은 500자 이하이어야 합니다.")
	String profileImageUrl
) {
}
