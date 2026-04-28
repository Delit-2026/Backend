package com.dealit.dealit.domain.member.controller;

import com.dealit.dealit.domain.member.dto.LoginIdCheckResponse;
import com.dealit.dealit.domain.member.dto.NicknameCheckResponse;
import com.dealit.dealit.domain.member.dto.SignUpRequest;
import com.dealit.dealit.domain.member.dto.SignUpResponse;
import com.dealit.dealit.domain.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "회원 API")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

	private final MemberService memberService;

	@Operation(summary = "회원가입", description = "신규 회원을 생성합니다.")
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public SignUpResponse signUp(@Valid @RequestBody SignUpRequest request) {
		return memberService.signUp(request);
	}

	@Operation(summary = "로그인 아이디 중복 확인", description = "회원가입 화면에서 로그인 아이디 사용 가능 여부를 확인합니다.")
	@GetMapping("/login-id/check")
	public LoginIdCheckResponse checkLoginId(
		@RequestParam
		@NotBlank(message = "로그인 아이디는 필수입니다.")
		@Size(max = 30, message = "로그인 아이디는 30자 이하이어야 합니다.")
		String loginId
	) {
		return memberService.checkLoginIdAvailability(loginId);
	}

	@Operation(summary = "닉네임 중복 확인", description = "프로필 설정 화면에서 닉네임 사용 가능 여부를 확인합니다.")
	@GetMapping("/nickname/check")
	public NicknameCheckResponse checkNickname(
		@RequestParam
		@NotBlank(message = "닉네임은 필수입니다.")
		@Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하이어야 합니다.")
		String nickname
	) {
		return memberService.checkNicknameAvailability(nickname);
	}
}
