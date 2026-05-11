package com.dealit.dealit.domain.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "일반 상품 구매 요청")
public record PurchaseRequest(
	@Schema(description = "중복 결제 방지용 멱등성 키(UUID 형식)", example = "550e8400-e29b-41d4-a716-446655440000")
	@NotBlank(message = "idempotencyKey는 필수입니다.")
	String idempotencyKey
) {
}
