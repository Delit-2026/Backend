package com.dealit.dealit.domain.purchase.controller;

import com.dealit.dealit.domain.purchase.dto.PurchaseRequest;
import com.dealit.dealit.domain.purchase.dto.PurchaseResponse;
import com.dealit.dealit.domain.purchase.service.PurchaseService;
import com.dealit.dealit.global.error.ErrorResponse;
import com.dealit.dealit.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Purchase", description = "일반 상품 딜릿머니 구매 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductPurchaseController {

	private final PurchaseService purchaseService;

	@Operation(
		summary = "일반 상품 딜릿머니 구매",
		description = """
			일반 상품을 구매자의 딜릿머니로 구매합니다.
			구매 성공 시 구매자 지갑에서 금액이 차감되고, 구매 기록이 PAID 상태로 저장되며, 상품은 SOLD 상태가 됩니다.
			idempotencyKey는 UUID 형식이어야 하며, 중복 결제 방지를 위해 사용됩니다.
			"""
	)
	@ApiResponse(responseCode = "200", description = "구매 성공", content = @Content(schema = @Schema(implementation = PurchaseResponse.class)))
	@ApiResponse(responseCode = "400", description = "요청 값 검증 실패 또는 idempotencyKey 형식 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "이메일 미인증 사용자", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "구매 불가 상품, 잔액 부족, 멱등성 키 충돌", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/{productId}/purchase")
	public PurchaseResponse purchase(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long productId,
		@Valid @RequestBody PurchaseRequest request
	) {
		return purchaseService.purchase(productId, member.memberId(), request.idempotencyKey());
	}
}
