package com.dealit.dealit.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMyLocationRequest(
	@NotBlank(message = "지역은 필수입니다.")
	@Size(max = 100, message = "지역은 100자 이하이어야 합니다.")
	String location
) {
}
