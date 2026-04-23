package com.dealit.dealit.domain.product.dto;

import com.dealit.dealit.domain.product.ProductSaleType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "일반 상품 등록 요청")
public record CreateProductRequest(
	@Schema(description = "상품명", example = "일반 판매 상품명")
	@NotBlank(message = "상품명은 필수입니다.")
	@Size(max = 100, message = "상품명은 100자 이하여야 합니다.")
	String name,

	@Schema(description = "상품 설명", example = "상품 설명")
	@NotBlank(message = "상품 설명은 필수입니다.")
	@Size(max = 2000, message = "상품 설명은 2000자 이하여야 합니다.")
	String description,

	@Schema(description = "판매 유형", example = "REGULAR")
	@NotNull(message = "판매 유형은 필수입니다.")
	ProductSaleType saleType,

	@Schema(description = "카테고리 ID", example = "19")
	@NotNull(message = "카테고리 ID는 필수입니다.")
	Long categoryId,

	@Schema(description = "일반 판매가", example = "12000")
	@NotNull(message = "판매가는 필수입니다.")
	BigDecimal price,

	@Schema(description = "가격 제안 허용 여부", example = "false")
	@NotNull(message = "가격 제안 허용 여부는 필수입니다.")
	Boolean allowOffer,

	@ArraySchema(schema = @Schema(implementation = ProductImagePayload.class), minItems = 1)
	@NotEmpty(message = "이미지는 최소 1장 이상 등록해야 합니다.")
	List<@Valid ProductImagePayload> images,

	@Schema(description = "거래 위치", example = "서울 강남구")
	@NotBlank(message = "거래 위치는 필수입니다.")
	@Size(max = 100, message = "거래 위치는 100자 이하여야 합니다.")
	String location,

	@Schema(description = "임시저장 ID", nullable = true)
	Long draftId
) {
}
