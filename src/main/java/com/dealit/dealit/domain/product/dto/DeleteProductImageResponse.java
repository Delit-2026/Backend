package com.dealit.dealit.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일반 상품 이미지 삭제 응답")
public record DeleteProductImageResponse(
	@Schema(description = "삭제된 이미지 ID", example = "301")
	Long imageId,

	@Schema(description = "삭제 여부", example = "true")
	boolean deleted
) {
}
