package com.dealit.dealit.domain.member.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email-verification")
public record EmailVerificationProperties(
	long codeTtlSeconds,
	long verifiedTtlSeconds,
	String subject,
	String senderAddress
) {
}
