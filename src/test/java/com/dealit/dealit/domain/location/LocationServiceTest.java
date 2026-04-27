package com.dealit.dealit.domain.location;

import com.dealit.dealit.domain.location.client.KakaoCoordToAddressResult;
import com.dealit.dealit.domain.location.client.KakaoLocalClient;
import com.dealit.dealit.domain.location.dto.ResolveLocationRequest;
import com.dealit.dealit.domain.location.dto.ResolvedLocationResponse;
import com.dealit.dealit.domain.location.service.LocationService;
import com.dealit.dealit.domain.member.LocationSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocationServiceTest {

	@Test
	void resolveReturnsStructuredGpsLocation() {
		KakaoLocalClient kakaoLocalClient = mock(KakaoLocalClient.class);
		LocationService locationService = new LocationService(kakaoLocalClient);
		ResolveLocationRequest request = new ResolveLocationRequest(
			new BigDecimal("37.5665000"),
			new BigDecimal("126.9780000")
		);

		when(kakaoLocalClient.resolve(eq(request.latitude()), eq(request.longitude())))
			.thenReturn(new KakaoCoordToAddressResult(
				"서울특별시 중구 세종대로 110",
				"서울특별시 중구 태평로1가 31",
				"04524",
				"서울특별시",
				"중구",
				"태평로1가"
			));

		ResolvedLocationResponse response = locationService.resolve(request);

		assertThat(response.location()).isEqualTo("서울특별시 중구 세종대로 110");
		assertThat(response.postalCode()).isEqualTo("04524");
		assertThat(response.roadAddress()).isEqualTo("서울특별시 중구 세종대로 110");
		assertThat(response.jibunAddress()).isEqualTo("서울특별시 중구 태평로1가 31");
		assertThat(response.detailAddress()).isNull();
		assertThat(response.latitude()).isEqualByComparingTo("37.5665000");
		assertThat(response.longitude()).isEqualByComparingTo("126.9780000");
		assertThat(response.locationSource()).isEqualTo(LocationSource.GPS);
		assertThat(response.region1DepthName()).isEqualTo("서울특별시");
		assertThat(response.region2DepthName()).isEqualTo("중구");
		assertThat(response.region3DepthName()).isEqualTo("태평로1가");
	}

	@Test
	void resolveFallsBackToJibunAddressWhenRoadAddressMissing() {
		KakaoLocalClient kakaoLocalClient = mock(KakaoLocalClient.class);
		LocationService locationService = new LocationService(kakaoLocalClient);
		ResolveLocationRequest request = new ResolveLocationRequest(
			new BigDecimal("37.1234567"),
			new BigDecimal("127.1234567")
		);

		when(kakaoLocalClient.resolve(eq(request.latitude()), eq(request.longitude())))
			.thenReturn(new KakaoCoordToAddressResult(
				null,
				"경기도 성남시 분당구 백현동 532",
				null,
				"경기도",
				"성남시 분당구",
				"백현동"
			));

		ResolvedLocationResponse response = locationService.resolve(request);

		assertThat(response.location()).isEqualTo("경기도 성남시 분당구 백현동 532");
		assertThat(response.roadAddress()).isNull();
		assertThat(response.jibunAddress()).isEqualTo("경기도 성남시 분당구 백현동 532");
		assertThat(response.locationSource()).isEqualTo(LocationSource.GPS);
	}
}
