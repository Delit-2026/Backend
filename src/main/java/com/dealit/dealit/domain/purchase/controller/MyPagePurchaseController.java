package com.dealit.dealit.domain.purchase.controller;

import com.dealit.dealit.domain.purchase.dto.MyPurchaseListResponse;
import com.dealit.dealit.domain.purchase.dto.MySaleListResponse;
import com.dealit.dealit.domain.purchase.entity.PurchaseStatus;
import com.dealit.dealit.domain.purchase.service.PurchaseService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "MyPage Purchase", description = "마이페이지 구매내역 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mypage")
public class MyPagePurchaseController {

	private final PurchaseService purchaseService;

	@Operation(summary = "내 구매내역 조회", description = "현재 로그인한 사용자의 구매내역을 페이지 단위로 조회합니다.")
	@ApiResponse(responseCode = "200", description = "내 구매내역 조회 성공")
	@GetMapping("/purchases")
	public MyPurchaseListResponse getMyPurchases(
		@AuthenticationPrincipal AuthenticatedMember member,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<PurchaseStatus> status
	) {
		return purchaseService.getMyPurchases(member.memberId(), page, size, status);
	}

	@Operation(summary = "내 판매내역 조회", description = "현재 로그인한 사용자의 판매내역을 페이지 단위로 조회합니다.")
	@ApiResponse(responseCode = "200", description = "내 판매내역 조회 성공")
	@GetMapping("/sales")
	public MySaleListResponse getMySales(
		@AuthenticationPrincipal AuthenticatedMember member,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<PurchaseStatus> status
	) {
		return purchaseService.getMySales(member.memberId(), page, size, status);
	}
}
