package com.dealit.dealit.domain.member.service;

public interface VerificationEmailSender {

	void send(String email, String code);
}
