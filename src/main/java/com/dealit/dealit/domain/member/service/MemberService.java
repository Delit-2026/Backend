package com.dealit.dealit.domain.member.service;

import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.dto.SignUpRequest;
import com.dealit.dealit.domain.member.dto.SignUpResponse;
import com.dealit.dealit.domain.member.exception.DuplicateMemberException;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public SignUpResponse signUp(SignUpRequest request) {
		validateDuplicate(request);

		Member member = Member.create(
			request.loginId().trim(),
			passwordEncoder.encode(request.password()),
			request.email().trim().toLowerCase(),
			null,
			normalizeBlank(request.name())
		);

		Member savedMember = memberRepository.saveAndFlush(member);
		savedMember.assignDefaultNickname();

		return new SignUpResponse(
			savedMember.getMemberId(),
			savedMember.getLoginId(),
			savedMember.getEmail(),
			savedMember.getNickname(),
			savedMember.getCreatedAt()
		);
	}

	private void validateDuplicate(SignUpRequest request) {
		String normalizedEmail = request.email().trim().toLowerCase();
		String normalizedLoginId = request.loginId().trim();

		if (memberRepository.existsByEmail(normalizedEmail)) {
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
}
