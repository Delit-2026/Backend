package com.dealit.dealit.domain.member.exception;

import org.springframework.http.HttpStatus;

public class EmailNotVerifiedException extends MemberException {

	public EmailNotVerifiedException() {
		super(
			"EMAIL_NOT_VERIFIED",
			"이메일 인증 후 이용할 수 있습니다.",
			HttpStatus.FORBIDDEN
		);
	}
}
