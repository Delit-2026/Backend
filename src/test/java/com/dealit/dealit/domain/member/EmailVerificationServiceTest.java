package com.dealit.dealit.domain.member;

import com.dealit.dealit.domain.member.config.EmailVerificationProperties;
import com.dealit.dealit.domain.member.dto.ConfirmEmailVerificationRequest;
import com.dealit.dealit.domain.member.dto.ConfirmEmailVerificationResponse;
import com.dealit.dealit.domain.member.dto.SendEmailVerificationRequest;
import com.dealit.dealit.domain.member.dto.SendEmailVerificationResponse;
import com.dealit.dealit.domain.member.exception.EmailVerificationCodeMismatchException;
import com.dealit.dealit.domain.member.exception.EmailVerificationCodeNotFoundException;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.member.service.EmailVerificationService;
import com.dealit.dealit.domain.member.service.VerificationEmailSender;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailVerificationServiceTest {

	@Test
	void sendStoresCodeAndReturnsExpiry() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		VerificationEmailSender emailSender = mock(VerificationEmailSender.class);
		MemberRepository memberRepository = mock(MemberRepository.class);
		EmailVerificationProperties properties = new EmailVerificationProperties(300L, 1800L, "Dealit 이메일 인증 코드", "noreply@dealit.com");

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(memberRepository.existsByEmail("user@dealit.com")).thenReturn(false);
		doNothing().when(emailSender).send(anyString(), anyString());

		EmailVerificationService service = new EmailVerificationService(redisTemplate, emailSender, properties, memberRepository);
		SendEmailVerificationResponse response = service.send(new SendEmailVerificationRequest("user@dealit.com"));

		assertThat(response.email()).isEqualTo("user@dealit.com");
		assertThat(response.expiresInSeconds()).isEqualTo(300L);
	}

	@Test
	void confirmStoresVerifiedMarker() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		VerificationEmailSender emailSender = mock(VerificationEmailSender.class);
		MemberRepository memberRepository = mock(MemberRepository.class);
		EmailVerificationProperties properties = new EmailVerificationProperties(300L, 1800L, "Dealit 이메일 인증 코드", "noreply@dealit.com");

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("email-verification:code:user@dealit.com")).thenReturn("123456");

		EmailVerificationService service = new EmailVerificationService(redisTemplate, emailSender, properties, memberRepository);
		ConfirmEmailVerificationResponse response = service.confirm(new ConfirmEmailVerificationRequest("user@dealit.com", "123456"));

		assertThat(response.email()).isEqualTo("user@dealit.com");
		assertThat(response.verified()).isTrue();
	}

	@Test
	void confirmFailsWhenCodeMissing() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		VerificationEmailSender emailSender = mock(VerificationEmailSender.class);
		MemberRepository memberRepository = mock(MemberRepository.class);
		EmailVerificationProperties properties = new EmailVerificationProperties(300L, 1800L, "Dealit 이메일 인증 코드", "noreply@dealit.com");

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("email-verification:code:user@dealit.com")).thenReturn(null);

		EmailVerificationService service = new EmailVerificationService(redisTemplate, emailSender, properties, memberRepository);

		assertThatThrownBy(() -> service.confirm(new ConfirmEmailVerificationRequest("user@dealit.com", "123456")))
			.isInstanceOf(EmailVerificationCodeNotFoundException.class);
	}

	@Test
	void confirmFailsWhenCodeMismatch() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		VerificationEmailSender emailSender = mock(VerificationEmailSender.class);
		MemberRepository memberRepository = mock(MemberRepository.class);
		EmailVerificationProperties properties = new EmailVerificationProperties(300L, 1800L, "Dealit 이메일 인증 코드", "noreply@dealit.com");

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("email-verification:code:user@dealit.com")).thenReturn("123456");

		EmailVerificationService service = new EmailVerificationService(redisTemplate, emailSender, properties, memberRepository);

		assertThatThrownBy(() -> service.confirm(new ConfirmEmailVerificationRequest("user@dealit.com", "654321")))
			.isInstanceOf(EmailVerificationCodeMismatchException.class);
	}
}
