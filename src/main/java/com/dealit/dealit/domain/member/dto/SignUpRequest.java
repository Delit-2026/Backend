package com.dealit.dealit.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "회원가입 요청")
public record SignUpRequest(
	@Schema(description = "로그인 아이디", example = "dealit_user")
	@NotBlank(message = "로그인 아이디는 필수입니다.")
	@Size(max = 30, message = "로그인 아이디는 30자 이하여야 합니다.")
	String loginId,

	@Schema(description = "비밀번호", example = "Password123!")
	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(min = 8, max = 255, message = "비밀번호는 8자 이상이어야 합니다.")
	@Pattern(
		regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$",
		message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다."
	)
	String password,

	@Schema(description = "이메일", example = "hong@example.com")
	@Email(message = "올바른 이메일 형식이어야 합니다.")
	@Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
	String email,

	@Schema(description = "이름", example = "홍길동")
	@Size(max = 30, message = "이름은 30자 이하여야 합니다.")
	String name,

	@Schema(description = "관심 카테고리 ID 목록(대분류만 허용)", example = "[1, 2]")
	List<Long> interestCategoryIds
) {
}
