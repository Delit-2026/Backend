package com.dealit.dealit.domain.purchase.controller;

import com.dealit.dealit.domain.purchase.dto.PurchaseCompletionResponse;
import com.dealit.dealit.domain.purchase.dto.PurchaseReceiptResponse;
import com.dealit.dealit.domain.purchase.service.PurchaseService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/purchases")
public class PurchaseController {

	private final PurchaseService purchaseService;

	@GetMapping("/{purchaseId}")
	public PurchaseReceiptResponse getReceipt(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long purchaseId
	) {
		return purchaseService.getReceipt(purchaseId, member.memberId());
	}

	@PostMapping("/{purchaseId}/buyer-complete")
	public PurchaseCompletionResponse completeByBuyer(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long purchaseId
	) {
		return purchaseService.completeByBuyer(purchaseId, member.memberId());
	}

	@PostMapping("/{purchaseId}/seller-complete")
	public PurchaseCompletionResponse completeBySeller(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long purchaseId
	) {
		return purchaseService.completeBySeller(purchaseId, member.memberId());
	}
}
