package com.dealit.dealit.domain.member.dto;

import com.dealit.dealit.domain.member.LocationSource;

public record UpdateMyLocationResponse(
	String location,
	String postalCode,
	String roadAddress,
	String jibunAddress,
	String detailAddress,
	LocationSource locationSource
) {
}
