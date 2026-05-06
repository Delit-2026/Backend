package com.dealit.dealit.domain.purchase.dto;

import jakarta.validation.constraints.NotBlank;

public record PurchaseRequest(
	@NotBlank(message = "idempotencyKey는 필수입니다.")
	String idempotencyKey
) {
}
