package com.dealit.dealit.domain.member.dto;

public record LoginIdCheckResponse(
	String loginId,
	boolean available
) {
}
