package com.dealit.dealit.domain.auction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "상품 이미지 메타데이터")
public record ProductImagePayload(
	@Schema(description = "업로드된 이미지 ID", example = "1")
	@NotNull(message = "이미지 ID는 필수입니다.")
	@Positive(message = "이미지 ID는 1 이상이어야 합니다.")
	Long imageId,

	@Schema(description = "이미지 URL", example = "http://localhost:8080/auction/images/1-sample.jpg")
	@NotBlank(message = "이미지 URL은 필수입니다.")
	String imageUrl,

	@Schema(description = "정렬 순서", example = "1")
	@NotNull(message = "정렬 순서는 필수입니다.")
	@Positive(message = "정렬 순서는 1 이상이어야 합니다.")
	Integer sortOrder
) {
}
