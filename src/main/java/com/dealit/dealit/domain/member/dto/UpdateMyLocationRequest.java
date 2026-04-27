package com.dealit.dealit.domain.member.dto;

import com.dealit.dealit.domain.member.LocationSource;
import jakarta.validation.constraints.Size;

public record UpdateMyLocationRequest(
	@Size(max = 100, message = "지역은 100자 이하여야 합니다.")
	String location,

	@Size(max = 10, message = "우편번호는 10자 이하여야 합니다.")
	String postalCode,

	@Size(max = 255, message = "도로명 주소는 255자 이하여야 합니다.")
	String roadAddress,

	@Size(max = 255, message = "지번 주소는 255자 이하여야 합니다.")
	String jibunAddress,

	@Size(max = 255, message = "상세 주소는 255자 이하여야 합니다.")
	String detailAddress,

	LocationSource locationSource
) {
}
