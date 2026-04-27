package com.dealit.dealit.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendEmailVerificationRequest(
	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "올바른 이메일 형식이어야 합니다.")
	@Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
	String email
) {
}
