package com.dealit.dealit.domain.member.dto;

import com.dealit.dealit.domain.member.LocationSource;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

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

	@DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
	@DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
	BigDecimal latitude,

	@DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
	@DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
	BigDecimal longitude,

	LocationSource locationSource
) {
}
