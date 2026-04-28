package com.dealit.dealit.global.config;

import com.dealit.dealit.domain.member.config.MailSenderProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

	@Bean
	public JavaMailSender javaMailSender(MailSenderProperties properties) {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(properties.host());
		mailSender.setPort(properties.port());
		mailSender.setUsername(properties.username());
		mailSender.setPassword(properties.password());
		if (properties.protocol() != null && !properties.protocol().isBlank()) {
			mailSender.setProtocol(properties.protocol());
		}

		Properties javaMailProperties = new Properties();
		javaMailProperties.put("mail.smtp.auth", String.valueOf(properties.auth()));
		javaMailProperties.put("mail.smtp.starttls.enable", String.valueOf(properties.starttlsEnable()));
		if (properties.properties() != null) {
			javaMailProperties.putAll(properties.properties());
		}
		mailSender.setJavaMailProperties(javaMailProperties);
		return mailSender;
	}
}
