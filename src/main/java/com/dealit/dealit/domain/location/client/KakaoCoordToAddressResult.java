package com.dealit.dealit.domain.location.client;

public record KakaoCoordToAddressResult(
	String roadAddress,
	String jibunAddress,
	String postalCode,
	String region1DepthName,
	String region2DepthName,
	String region3DepthName
) {
}
