package com.dealit.dealit.domain.product.dto;

import com.dealit.dealit.domain.product.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "일반 상품 수정 상세 응답")
public record ProductEditDetailResponse(
	@Schema(description = "상품 ID", example = "10")
	Long productId,

	@Schema(description = "상품명", example = "아이폰 14 Pro")
	String name,

	@Schema(description = "상품 설명", example = "거의 새 상품입니다.")
	String description,

	@Schema(description = "카테고리 ID", example = "16")
	Long categoryId,

	@Schema(description = "카테고리명", example = "iPhone")
	String categoryName,

	@Schema(description = "거래 위치", example = "서울특별시 강남구")
	String location,

	@Schema(description = "상품 이미지 목록")
	List<ProductEditImageResponse> images,

	@Schema(description = "판매 가격", example = "850000")
	BigDecimal price,

	@Schema(description = "가격 제안 허용 여부", example = "false")
	boolean allowOffer,

	@Schema(description = "상품 상태", example = "ON_SALE")
	ProductStatus status,

	@Schema(description = "수정 가능 여부", example = "true")
	boolean editable
) {
	public record ProductEditImageResponse(
		@Schema(description = "이미지 ID", example = "1")
		Long imageId,

		@Schema(description = "이미지 URL", example = "https://example.com/product/images/1.jpg")
		String imageUrl,

		@Schema(description = "정렬 순서", example = "1")
		Integer sortOrder
	) {
	}
}
