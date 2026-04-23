package com.dealit.dealit.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "일반 상품 이미지 메타데이터")
public record ProductImagePayload(
	@Schema(description = "업로드된 이미지 ID", example = "1")
	@NotNull(message = "imageId는 필수입니다.")
	Long imageId,

	@Schema(description = "이미지 URL", example = "http://localhost:8080/product/images/1-sample.jpg")
	@NotBlank(message = "imageUrl은 필수입니다.")
	String imageUrl,

	@Schema(description = "정렬 순서", example = "1")
	@Min(value = 1, message = "sortOrder는 1 이상이어야 합니다.")
	int sortOrder
) {
}
