package com.dealit.dealit.domain.member.service;

import com.dealit.dealit.domain.member.config.EmailVerificationProperties;
import com.dealit.dealit.domain.member.exception.EmailVerificationSendFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmtpVerificationEmailSender implements VerificationEmailSender {

	private final MailSender mailSender;
	private final EmailVerificationProperties properties;

	@Override
	public void send(String email, String code) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(email);
		message.setSubject(properties.subject());
		if (properties.senderAddress() != null && !properties.senderAddress().isBlank()) {
			message.setFrom(properties.senderAddress());
		}
		message.setText("""
			Dealit 이메일 인증 코드입니다.

			인증 코드: %s

			인증 코드는 %d초 동안 유효합니다.
			""".formatted(code, properties.codeTtlSeconds()));

		try {
			mailSender.send(message);
		} catch (MailException exception) {
			throw new EmailVerificationSendFailedException("이메일 인증 코드 발송에 실패했습니다.");
		}
	}
}
