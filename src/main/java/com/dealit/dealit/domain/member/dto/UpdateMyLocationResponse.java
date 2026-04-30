package com.dealit.dealit.domain.member.dto;

import com.dealit.dealit.domain.member.LocationSource;
import java.math.BigDecimal;

public record UpdateMyLocationResponse(
	String location,
	String postalCode,
	String roadAddress,
	String jibunAddress,
	String detailAddress,
	BigDecimal latitude,
	BigDecimal longitude,
	LocationSource locationSource
) {
}
