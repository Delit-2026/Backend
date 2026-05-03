package com.dealit.dealit.domain.wallet.controller;

import com.dealit.dealit.domain.wallet.dto.WalletAmountRequest;
import com.dealit.dealit.domain.wallet.dto.WalletLedgerListResponse;
import com.dealit.dealit.domain.wallet.dto.WalletResponse;
import com.dealit.dealit.domain.wallet.service.WalletService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wallet", description = "딜릿머니 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallet")
public class WalletController {

	private final WalletService walletService;

	@Operation(summary = "내 딜릿머니 지갑 조회")
	@ApiResponse(responseCode = "200", description = "내 딜릿머니 지갑 조회 성공")
	@GetMapping
	public WalletResponse getMyWallet(@AuthenticationPrincipal AuthenticatedMember member) {
		return walletService.getMyWallet(member.memberId());
	}

	@Operation(summary = "딜릿머니 내역 조회")
	@ApiResponse(responseCode = "200", description = "딜릿머니 내역 조회 성공")
	@GetMapping("/ledgers")
	public WalletLedgerListResponse getMyLedgers(
		@AuthenticationPrincipal AuthenticatedMember member,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return walletService.getMyLedgers(member.memberId(), page, size);
	}

	@Operation(summary = "딜릿머니 임시 충전")
	@ApiResponse(responseCode = "200", description = "딜릿머니 임시 충전 성공")
	@PostMapping("/charge")
	public WalletResponse charge(
		@AuthenticationPrincipal AuthenticatedMember member,
		@Valid @RequestBody WalletAmountRequest request
	) {
		return walletService.charge(member.memberId(), request.amount());
	}

	@Operation(summary = "딜릿머니 환불")
	@ApiResponse(responseCode = "200", description = "딜릿머니 환불 성공")
	@PostMapping("/refund")
	public WalletResponse refund(
		@AuthenticationPrincipal AuthenticatedMember member,
		@Valid @RequestBody WalletAmountRequest request
	) {
		return walletService.refund(member.memberId(), request.amount());
	}

	@Operation(summary = "딜릿머니 출금 신청")
	@ApiResponse(responseCode = "200", description = "딜릿머니 출금 신청 성공")
	@PostMapping("/withdraw")
	public WalletResponse withdraw(
		@AuthenticationPrincipal AuthenticatedMember member,
		@Valid @RequestBody WalletAmountRequest request
	) {
		return walletService.withdraw(member.memberId(), request.amount());
	}
}
