package com.dealit.dealit.domain.member.service;

import com.dealit.dealit.domain.member.dto.LoginIdCheckResponse;
import com.dealit.dealit.domain.member.dto.NicknameCheckResponse;
import com.dealit.dealit.domain.member.dto.SignUpRequest;
import com.dealit.dealit.domain.member.dto.SignUpResponse;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.exception.DuplicateMemberException;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailVerificationService emailVerificationService;

	@Transactional
	public SignUpResponse signUp(SignUpRequest request) {
		validateDuplicate(request);

		String normalizedEmail = normalizeEmail(request.email());
		boolean verified = normalizedEmail != null
			&& emailVerificationService.consumeVerifiedStatus(normalizedEmail);

		Member member = Member.create(
			request.loginId().trim(),
			passwordEncoder.encode(request.password()),
			normalizedEmail,
			null,
			normalizeBlank(request.name()),
			verified
		);

		Member savedMember = memberRepository.save(member);
		savedMember.assignDefaultNickname();

		return new SignUpResponse(
			savedMember.getMemberId(),
			savedMember.getLoginId(),
			savedMember.getEmail(),
			savedMember.getNickname(),
			savedMember.getCreatedAt()
		);
	}

	@Transactional(readOnly = true)
	public LoginIdCheckResponse checkLoginIdAvailability(String loginId) {
		String normalizedLoginId = loginId.trim();
		return new LoginIdCheckResponse(
			normalizedLoginId,
			!memberRepository.existsByLoginId(normalizedLoginId)
		);
	}

	@Transactional(readOnly = true)
	public NicknameCheckResponse checkNicknameAvailability(String nickname) {
		String normalizedNickname = nickname.trim();
		return new NicknameCheckResponse(
			normalizedNickname,
			!memberRepository.existsByNickname(normalizedNickname)
		);
	}

	private void validateDuplicate(SignUpRequest request) {
		String normalizedEmail = normalizeEmail(request.email());
		String normalizedLoginId = request.loginId().trim();

		if (normalizedEmail != null && memberRepository.existsByEmail(normalizedEmail)) {
			throw new DuplicateMemberException("이미 가입된 이메일입니다.");
		}

		if (memberRepository.existsByLoginId(normalizedLoginId)) {
			throw new DuplicateMemberException("이미 사용 중인 로그인 아이디입니다.");
		}
	}

	private String normalizeBlank(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String normalizeEmail(String email) {
		String normalized = normalizeBlank(email);
		return normalized == null ? null : normalized.toLowerCase();
	}
}
