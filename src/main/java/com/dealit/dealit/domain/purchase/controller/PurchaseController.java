package com.dealit.dealit.domain.purchase.controller;

import com.dealit.dealit.domain.purchase.dto.PurchaseCompletionResponse;
import com.dealit.dealit.domain.purchase.dto.PurchaseReceiptResponse;
import com.dealit.dealit.domain.purchase.service.PurchaseService;
import com.dealit.dealit.global.error.ErrorResponse;
import com.dealit.dealit.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Purchase", description = "일반 상품 구매 내역, 영수증, 거래 완료 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/purchases")
public class PurchaseController {

	private final PurchaseService purchaseService;

	@Operation(summary = "구매 영수증 조회", description = "purchaseId 기준으로 구매 영수증을 조회합니다. 구매자와 판매자만 조회할 수 있습니다.")
	@ApiResponse(responseCode = "200", description = "영수증 조회 성공", content = @Content(schema = @Schema(implementation = PurchaseReceiptResponse.class)))
	@ApiResponse(responseCode = "403", description = "구매자 또는 판매자가 아닌 사용자", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "구매 내역 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/{purchaseId}")
	public PurchaseReceiptResponse getReceipt(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long purchaseId
	) {
		return purchaseService.getReceipt(purchaseId, member.memberId());
	}

	@Operation(
		summary = "구매자 거래 완료 처리",
		description = "구매자가 거래 완료 버튼을 눌렀을 때 buyerCompletedAt을 저장합니다. 판매자도 완료한 상태라면 구매 상태가 COMPLETED가 되고 판매자 정산이 수행됩니다."
	)
	@ApiResponse(responseCode = "200", description = "구매자 완료 처리 성공", content = @Content(schema = @Schema(implementation = PurchaseCompletionResponse.class)))
	@ApiResponse(responseCode = "403", description = "구매자가 아닌 사용자", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "구매 내역 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "완료 처리할 수 없는 구매 상태", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/{purchaseId}/buyer-complete")
	public PurchaseCompletionResponse completeByBuyer(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long purchaseId
	) {
		return purchaseService.completeByBuyer(purchaseId, member.memberId());
	}

	@Operation(summary = "구매자 수령확정 처리", description = "판매자가 발송 완료 처리한 뒤 구매자가 물건을 받았어요 버튼을 눌렀을 때 거래를 완료하고 판매자 정산을 수행합니다.")
	@ApiResponse(responseCode = "200", description = "구매자 수령확정 처리 성공", content = @Content(schema = @Schema(implementation = PurchaseCompletionResponse.class)))
	@ApiResponse(responseCode = "403", description = "구매자가 아닌 사용자", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "구매 내역 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "수령확정 처리할 수 없는 구매 상태", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/{purchaseId}/receive")
	public PurchaseCompletionResponse receiveByBuyer(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long purchaseId
	) {
		return purchaseService.receiveByBuyer(purchaseId, member.memberId());
	}

	@Operation(
		summary = "판매자 거래 완료 처리",
		description = "판매자가 거래 완료 버튼을 눌렀을 때 sellerCompletedAt을 저장합니다. 구매자도 완료한 상태라면 구매 상태가 COMPLETED가 되고 판매자 정산이 수행됩니다."
	)
	@ApiResponse(responseCode = "200", description = "판매자 완료 처리 성공", content = @Content(schema = @Schema(implementation = PurchaseCompletionResponse.class)))
	@ApiResponse(responseCode = "403", description = "판매자가 아닌 사용자", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "구매 내역 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "완료 처리할 수 없는 구매 상태", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/{purchaseId}/seller-complete")
	public PurchaseCompletionResponse completeBySeller(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long purchaseId
	) {
		return purchaseService.completeBySeller(purchaseId, member.memberId());
	}

	@Operation(summary = "판매자 발송 완료 처리", description = "판매자가 물건을 보냈어요 버튼을 눌렀을 때 거래 상태를 SHIPPED로 변경하고 구매자 수령확정 버튼을 활성화할 수 있게 합니다.")
	@ApiResponse(responseCode = "200", description = "판매자 발송 완료 처리 성공", content = @Content(schema = @Schema(implementation = PurchaseCompletionResponse.class)))
	@ApiResponse(responseCode = "403", description = "판매자가 아닌 사용자", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "구매 내역 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "발송 처리할 수 없는 구매 상태", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/{purchaseId}/ship")
	public PurchaseCompletionResponse shipBySeller(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long purchaseId
	) {
		return purchaseService.shipBySeller(purchaseId, member.memberId());
	}
}
