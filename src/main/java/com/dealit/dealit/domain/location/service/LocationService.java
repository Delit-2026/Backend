package com.dealit.dealit.domain.location.service;

import com.dealit.dealit.domain.location.client.KakaoCoordToAddressResult;
import com.dealit.dealit.domain.location.client.KakaoLocalClient;
import com.dealit.dealit.domain.location.dto.ResolveLocationRequest;
import com.dealit.dealit.domain.location.dto.ResolvedLocationResponse;
import com.dealit.dealit.domain.member.LocationSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {

	private final KakaoLocalClient kakaoLocalClient;

	public ResolvedLocationResponse resolve(ResolveLocationRequest request) {
		KakaoCoordToAddressResult result = kakaoLocalClient.resolve(request.latitude(), request.longitude());
		String location = result.roadAddress() != null ? result.roadAddress() : result.jibunAddress();

		return new ResolvedLocationResponse(
			location,
			result.postalCode(),
			result.roadAddress(),
			result.jibunAddress(),
			null,
			request.latitude(),
			request.longitude(),
			LocationSource.GPS,
			result.region1DepthName(),
			result.region2DepthName(),
			result.region3DepthName()
		);
	}
}
