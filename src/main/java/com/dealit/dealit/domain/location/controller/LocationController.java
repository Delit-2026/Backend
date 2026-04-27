package com.dealit.dealit.domain.location.controller;

import com.dealit.dealit.domain.location.dto.ResolveLocationRequest;
import com.dealit.dealit.domain.location.dto.ResolvedLocationResponse;
import com.dealit.dealit.domain.location.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Location", description = "위치 보조 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/locations")
public class LocationController {

	private final LocationService locationService;

	@Operation(summary = "좌표로 주소 변환", description = "위도와 경도를 기반으로 카카오 좌표-주소 변환 API를 호출해 주소 정보를 반환합니다.")
	@PostMapping("/resolve")
	public ResolvedLocationResponse resolve(@Valid @RequestBody ResolveLocationRequest request) {
		return locationService.resolve(request);
	}
}
