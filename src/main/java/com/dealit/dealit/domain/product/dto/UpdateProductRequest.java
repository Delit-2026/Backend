package com.dealit.dealit.domain.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProductRequest(
	@NotBlank(message = "상품명은 필수입니다.")
	@Size(max = 100, message = "상품명은 100자 이하여야 합니다.")
	String name,

	@NotBlank(message = "상품 설명은 필수입니다.")
	@Size(max = 2000, message = "상품 설명은 2000자 이하여야 합니다.")
	String description,

	@NotNull(message = "카테고리는 필수입니다.")
	Long categoryId,

	@NotNull(message = "판매 가격은 필수입니다.")
	@DecimalMin(value = "0.01", message = "판매 가격은 0보다 커야 합니다.")
	BigDecimal price,

	@NotNull(message = "가격 제안 허용 여부는 필수입니다.")
	Boolean allowOffer,

	@NotBlank(message = "거래 위치는 필수입니다.")
	@Size(max = 100, message = "거래 위치는 100자 이하여야 합니다.")
	String location,

	@NotEmpty(message = "상품 이미지는 최소 1개 이상 필요합니다.")
	@Valid
	List<ProductImagePayload> images
) {
}
