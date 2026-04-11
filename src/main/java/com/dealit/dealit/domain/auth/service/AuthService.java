package com.dealit.dealit.domain.auth.service;

import com.dealit.dealit.domain.auth.dto.CurrentMemberResponse;
import com.dealit.dealit.domain.auth.dto.LoginRequest;
import com.dealit.dealit.domain.auth.dto.LoginResponse;
import com.dealit.dealit.domain.auth.exception.InvalidCredentialsException;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.global.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		Member member = memberRepository.findByLoginIdAndDeletedAtIsNull(request.loginId().trim())
			.orElseThrow(() -> new InvalidCredentialsException("로그인 아이디 또는 비밀번호가 올바르지 않습니다."));

		if (!passwordEncoder.matches(request.password(), member.getPassword())) {
			throw new InvalidCredentialsException("로그인 아이디 또는 비밀번호가 올바르지 않습니다.");
		}

		return new LoginResponse(
			jwtService.generateAccessToken(member),
			"Bearer",
			jwtService.getAccessTokenExpirationMs()
		);
	}

	@Transactional(readOnly = true)
	public CurrentMemberResponse getCurrentMember(Long memberId) {
		Member member = memberRepository.findByMemberIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new InvalidCredentialsException("존재하지 않는 회원입니다."));

		return new CurrentMemberResponse(
			member.getMemberId(),
			member.getLoginId(),
			member.getEmail(),
			member.getNickname()
		);
	}
}
