package com.dealit.dealit.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "로그인 요청")
public record LoginRequest(
	@Schema(description = "로그인 아이디", example = "dealit-user")
	@NotBlank(message = "로그인 아이디는 필수입니다.")
	@Size(max = 30, message = "로그인 아이디는 30자 이하여야 합니다.")
	String loginId,

	@Schema(description = "비밀번호", example = "Password123!")
	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(min = 8, max = 30, message = "비밀번호는 8자 이상 30자 이하여야 합니다.")
	String password
) {
}
