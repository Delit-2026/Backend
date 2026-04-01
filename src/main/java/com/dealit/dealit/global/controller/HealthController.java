//앱 상태 확인용 헬스 체크 API를 제공

package com.dealit.dealit.global.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@Tag(name = "Health", description = "Application health check")
@RestController
@RequestMapping("/api/v1")
public class HealthController {

	@Operation(summary = "Health check", description = "Returns basic application status")
	@GetMapping("/health")
	public Map<String, Object> health() {
		return Map.of(
			"status", "UP",
			"service", "dealit-backend",
			"timestamp", OffsetDateTime.now()
		);
	}
}
