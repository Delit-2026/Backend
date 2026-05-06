package com.dealit.dealit.domain.purchase.controller;

import com.dealit.dealit.domain.purchase.dto.PurchaseRequest;
import com.dealit.dealit.domain.purchase.dto.PurchaseResponse;
import com.dealit.dealit.domain.purchase.service.PurchaseService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductPurchaseController {

	private final PurchaseService purchaseService;

	@PostMapping("/{productId}/purchase")
	public PurchaseResponse purchase(
		@AuthenticationPrincipal AuthenticatedMember member,
		@PathVariable Long productId,
		@Valid @RequestBody PurchaseRequest request
	) {
		return purchaseService.purchase(productId, member.memberId(), request.idempotencyKey());
	}
}
