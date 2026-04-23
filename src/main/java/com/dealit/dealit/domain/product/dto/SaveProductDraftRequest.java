package com.dealit.dealit.domain.product.dto;

import com.dealit.dealit.domain.product.ProductSaleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "일반 상품 임시저장 요청")
public record SaveProductDraftRequest(
	@Schema(description = "상품명", example = "일반 판매 상품명")
	@Size(max = 100, message = "상품명은 100자 이하여야 합니다.")
	String name,

	@Schema(description = "상품 설명", example = "상품 설명")
	@Size(max = 2000, message = "상품 설명은 2000자 이하여야 합니다.")
	String description,

	@Schema(description = "판매 유형", example = "REGULAR")
	ProductSaleType saleType,

	@Schema(description = "카테고리 ID", example = "19")
	Long categoryId,

	@Schema(description = "일반 판매가", example = "12000", nullable = true)
	BigDecimal price,

	@Schema(description = "가격 제안 허용 여부", example = "false", nullable = true)
	Boolean allowOffer,

	@Schema(description = "상품 이미지 목록", nullable = true)
	List<@Valid ProductImagePayload> images,

	@Schema(description = "거래 위치", example = "서울 강남구")
	@Size(max = 100, message = "거래 위치는 100자 이하여야 합니다.")
	String location,

	@Schema(description = "임시저장 ID", nullable = true)
	Long draftId
) {
	public SaveProductDraftRequest(CreateProductRequest request) {
		this(
			request.name(),
			request.description(),
			request.saleType(),
			request.categoryId(),
			request.price(),
			request.allowOffer(),
			request.images(),
			request.location(),
			request.draftId()
		);
	}
}
