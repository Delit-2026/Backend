package com.dealit.dealit.domain.member.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "app.mail")
public record MailSenderProperties(
	String host,
	int port,
	String username,
	String password,
	String protocol,
	boolean auth,
	boolean starttlsEnable,
	Map<String, String> properties
) {
}
