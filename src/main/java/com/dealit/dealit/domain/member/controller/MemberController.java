package com.dealit.dealit.domain.member.controller;

import com.dealit.dealit.domain.member.dto.SignUpRequest;
import com.dealit.dealit.domain.member.dto.SignUpResponse;
import com.dealit.dealit.domain.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "회원 API")
@RestController
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
}
