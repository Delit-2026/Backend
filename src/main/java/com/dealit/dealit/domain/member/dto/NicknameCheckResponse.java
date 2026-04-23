package com.dealit.dealit.domain.member.dto;

public record NicknameCheckResponse(
	String nickname,
	boolean available
) {
}
