package com.dealit.dealit.domain.location.dto;

import com.dealit.dealit.domain.member.LocationSource;

public record ResolvedLocationResponse(
	String location,
	String postalCode,
	String roadAddress,
	String jibunAddress,
	String detailAddress,
	LocationSource locationSource,
	String region1DepthName,
	String region2DepthName,
	String region3DepthName
) {
}
