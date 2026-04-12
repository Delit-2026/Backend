package com.dealit.dealit.domain.auth.controller;

import com.dealit.dealit.domain.auth.dto.CurrentMemberResponse;
import com.dealit.dealit.domain.auth.dto.LoginRequest;
import com.dealit.dealit.domain.auth.dto.LoginResponse;
import com.dealit.dealit.domain.auth.service.AuthService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	@Operation(summary = "로그인", description = "로그인 성공 시 JWT Access Token을 발급합니다.")
	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@Operation(summary = "내 정보 조회", description = "현재 인증된 회원의 기본 정보를 조회합니다.")
	@GetMapping("/me")
	public CurrentMemberResponse me(@AuthenticationPrincipal AuthenticatedMember member) {
		return authService.getCurrentMember(member.memberId());
	}
}
