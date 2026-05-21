package com.dealit.dealit.domain.recentproduct.controller;

import com.dealit.dealit.domain.recentproduct.dto.RecentProductListResponse;
import com.dealit.dealit.domain.recentproduct.service.RecentProductService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recent Products", description = "최근 본 상품 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/recent-products")
public class RecentProductController {

	private final RecentProductService recentProductService;

	@Operation(
		summary = "Recent Products",
		description = "로그인 사용자의 최근 본 일반상품과 경매상품을 최신 조회순으로 함께 조회합니다."
	)
	@ApiResponse(responseCode = "200", description = "최근 본 상품 조회 성공")
	@GetMapping
	public RecentProductListResponse getRecentProducts(
		@AuthenticationPrincipal AuthenticatedMember member,
		@RequestParam(defaultValue = "20") int size
	) {
		return recentProductService.findRecentProducts(member.memberId(), size);
	}
}
