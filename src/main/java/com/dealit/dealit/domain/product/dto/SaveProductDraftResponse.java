package com.dealit.dealit.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "일반 상품 임시저장 응답")
public record SaveProductDraftResponse(
	@Schema(description = "임시저장 ID", example = "10")
	Long draftId,

	@Schema(description = "저장 시각", example = "2026-04-12T20:00:00Z")
	OffsetDateTime savedAt
) {
}
