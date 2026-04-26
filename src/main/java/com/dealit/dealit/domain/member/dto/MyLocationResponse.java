package com.dealit.dealit.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 로그인한 사용자의 지역 정보 응답")
public record MyLocationResponse(
	@Schema(description = "지역", example = "서울 강남구", nullable = true)
	String location
) {
}
