package com.dealit.dealit.domain.auction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "상품 이미지 메타데이터")
public record ProductImagePayload(
	@Schema(description = "업로드된 이미지 ID", example = "1")
	@NotNull(message = "imageId is required.")
	@Positive(message = "imageId must be greater than 0.")
	Long imageId,

	@Schema(description = "이미지 URL", example = "https://cdn.dealit.local/auction/images/1.jpg")
	@NotBlank(message = "imageUrl is required.")
	String imageUrl,

	@Schema(description = "정렬 순서", example = "1")
	@NotNull(message = "sortOrder is required.")
	@Positive(message = "sortOrder must be greater than 0.")
	Integer sortOrder
) {
}
