package com.dealit.dealit.domain.member.service;

import com.dealit.dealit.domain.auth.exception.InvalidCredentialsException;
import com.dealit.dealit.domain.member.config.EmailVerificationProperties;
import com.dealit.dealit.domain.member.dto.ConfirmEmailVerificationRequest;
import com.dealit.dealit.domain.member.dto.ConfirmEmailVerificationResponse;
import com.dealit.dealit.domain.member.dto.SendEmailVerificationRequest;
import com.dealit.dealit.domain.member.dto.SendEmailVerificationResponse;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.exception.DuplicateMemberException;
import com.dealit.dealit.domain.member.exception.EmailVerificationCodeMismatchException;
import com.dealit.dealit.domain.member.exception.EmailVerificationCodeNotFoundException;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final StringRedisTemplate stringRedisTemplate;
	private final VerificationEmailSender verificationEmailSender;
	private final EmailVerificationProperties properties;
	private final MemberRepository memberRepository;

	@Transactional
	public SendEmailVerificationResponse send(SendEmailVerificationRequest request) {
		String email = normalizeEmail(request.email());

		String code = generateCode();
		stringRedisTemplate.opsForValue().set(codeKey(email), code, Duration.ofSeconds(properties.codeTtlSeconds()));
		stringRedisTemplate.delete(verifiedKey(email));
		verificationEmailSender.send(email, code);

		return new SendEmailVerificationResponse(email, properties.codeTtlSeconds());
	}

	@Transactional
	public ConfirmEmailVerificationResponse confirm(ConfirmEmailVerificationRequest request, Long memberId) {
		String email = normalizeEmail(request.email());
		String storedCode = stringRedisTemplate.opsForValue().get(codeKey(email));
		if (storedCode == null) {
			throw new EmailVerificationCodeNotFoundException("인증 코드가 없거나 만료되었습니다.");
		}

		if (!storedCode.equals(request.code().trim())) {
			throw new EmailVerificationCodeMismatchException("인증 코드가 올바르지 않습니다.");
		}

		stringRedisTemplate.delete(codeKey(email));
		stringRedisTemplate.opsForValue().set(verifiedKey(email), "true", Duration.ofSeconds(properties.verifiedTtlSeconds()));

		if (memberId != null) {
			Member member = memberRepository.findByMemberIdAndDeletedAtIsNull(memberId)
				.orElseThrow(() -> new InvalidCredentialsException("존재하지 않는 회원입니다."));

			memberRepository.findByEmailAndDeletedAtIsNull(email)
				.filter(existingMember -> !existingMember.getMemberId().equals(memberId))
				.ifPresent(existingMember -> {
					throw new DuplicateMemberException("이미 가입된 이메일입니다.");
				});

			member.updateEmailAndVerify(email);
		} else {
			memberRepository.findByEmailAndDeletedAtIsNull(email)
				.ifPresent(Member::verifyEmail);
		}

		return new ConfirmEmailVerificationResponse(email, true);
	}

	@Transactional
	public boolean consumeVerifiedStatus(String email) {
		String normalizedEmail = normalizeEmail(email);
		Boolean verified = stringRedisTemplate.hasKey(verifiedKey(normalizedEmail));
		if (Boolean.TRUE.equals(verified)) {
			stringRedisTemplate.delete(verifiedKey(normalizedEmail));
			return true;
		}
		return false;
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}

	private String generateCode() {
		return String.format("%06d", RANDOM.nextInt(1_000_000));
	}

	private String codeKey(String email) {
		return "email-verification:code:" + email;
	}

	private String verifiedKey(String email) {
		return "email-verification:verified:" + email;
	}
}
