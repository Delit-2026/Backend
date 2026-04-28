package com.dealit.dealit.domain.location.client;

import com.dealit.dealit.domain.location.exception.LocationResolveFailedException;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class KakaoLocalRestClient implements KakaoLocalClient {

	private static final String INPUT_COORD = "WGS84";

	private final KakaoLocalProperties properties;
	private final RestClient.Builder restClientBuilder;

	@Override
	public KakaoCoordToAddressResult resolve(BigDecimal latitude, BigDecimal longitude) {
		try {
			CoordToAddressResponse response = restClientBuilder
				.baseUrl(properties.normalizedBaseUrl())
				.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.build()
				.get()
				.uri(uriBuilder -> uriBuilder
					.path("/v2/local/geo/coord2address.json")
					.queryParam("x", longitude)
					.queryParam("y", latitude)
					.queryParam("input_coord", INPUT_COORD)
					.build())
				.header(HttpHeaders.AUTHORIZATION, properties.authorizationHeaderValue())
				.retrieve()
				.body(CoordToAddressResponse.class);

			if (response == null || response.documents() == null || response.documents().isEmpty()) {
				throw new LocationResolveFailedException("좌표에 해당하는 주소를 찾을 수 없습니다.");
			}

			Document document = response.documents().getFirst();
			Address address = document.address();
			RoadAddress roadAddress = document.roadAddress();

			if (address == null && roadAddress == null) {
				throw new LocationResolveFailedException("좌표에 해당하는 주소를 찾을 수 없습니다.");
			}

			String region1DepthName = roadAddress != null ? roadAddress.region1DepthName() : address.region1DepthName();
			String region2DepthName = roadAddress != null ? roadAddress.region2DepthName() : address.region2DepthName();
			String region3DepthName = roadAddress != null ? roadAddress.region3DepthName() : address.region3DepthName();

			return new KakaoCoordToAddressResult(
				roadAddress != null ? roadAddress.addressName() : null,
				address != null ? address.addressName() : null,
				roadAddress != null ? roadAddress.zoneNo() : null,
				region1DepthName,
				region2DepthName,
				region3DepthName
			);
		} catch (RestClientResponseException exception) {
			throw new LocationResolveFailedException(
				"카카오 위치 변환 API 호출에 실패했습니다. status=%d, body=%s"
					.formatted(exception.getStatusCode().value(), exception.getResponseBodyAsString())
			);
		} catch (RestClientException exception) {
			throw new LocationResolveFailedException("카카오 위치 변환 API 호출에 실패했습니다.");
		}
	}

	private record CoordToAddressResponse(
		List<Document> documents
	) {
	}

	private record Document(
		@JsonProperty("road_address")
		RoadAddress roadAddress,
		Address address
	) {
	}

	private record RoadAddress(
		@JsonProperty("address_name")
		String addressName,
		@JsonProperty("region_1depth_name")
		String region1DepthName,
		@JsonProperty("region_2depth_name")
		String region2DepthName,
		@JsonProperty("region_3depth_name")
		String region3DepthName,
		@JsonProperty("zone_no")
		String zoneNo
	) {
	}

	private record Address(
		@JsonProperty("address_name")
		String addressName,
		@JsonProperty("region_1depth_name")
		String region1DepthName,
		@JsonProperty("region_2depth_name")
		String region2DepthName,
		@JsonProperty("region_3depth_name")
		String region3DepthName
	) {
	}
}
