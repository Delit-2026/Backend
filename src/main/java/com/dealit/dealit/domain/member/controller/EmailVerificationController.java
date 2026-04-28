package com.dealit.dealit.domain.member.controller;

import com.dealit.dealit.domain.member.dto.ConfirmEmailVerificationRequest;
import com.dealit.dealit.domain.member.dto.ConfirmEmailVerificationResponse;
import com.dealit.dealit.domain.member.dto.SendEmailVerificationRequest;
import com.dealit.dealit.domain.member.dto.SendEmailVerificationResponse;
import com.dealit.dealit.domain.member.service.EmailVerificationService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Email Verification", description = "이메일 인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/email/verification")
public class EmailVerificationController {

	private final EmailVerificationService emailVerificationService;

	@Operation(summary = "이메일 인증 코드 발송", description = "회원가입 또는 프로필 수정에 사용할 이메일 인증 코드를 발송합니다.")
	@PostMapping("/send")
	@ResponseStatus(HttpStatus.CREATED)
	public SendEmailVerificationResponse send(@Valid @RequestBody SendEmailVerificationRequest request) {
		return emailVerificationService.send(request);
	}

	@Operation(summary = "이메일 인증 코드 확인", description = "발송된 이메일 인증 코드를 검증합니다.")
	@PostMapping("/confirm")
	public ConfirmEmailVerificationResponse confirm(
		@AuthenticationPrincipal AuthenticatedMember member,
		@Valid @RequestBody ConfirmEmailVerificationRequest request
	) {
		return emailVerificationService.confirm(request, member == null ? null : member.memberId());
	}
}
