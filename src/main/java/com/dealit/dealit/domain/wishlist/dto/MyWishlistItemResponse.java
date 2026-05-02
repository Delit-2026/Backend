package com.dealit.dealit.domain.wishlist.dto;

import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "찜 목록 항목")
public record MyWishlistItemResponse(
	@Schema(description = "상품 ID", example = "10")
	Long productId,

	@Schema(description = "상품명", example = "아이폰 14 Pro")
	String name,

	@Schema(description = "판매 유형", example = "REGULAR")
	ProductSaleType saleType,

	@Schema(description = "상품 상태", example = "ON_SALE")
	ProductStatus status,

	@Schema(description = "대표 이미지 URL", example = "https://example.com/product/images/10.jpg")
	String thumbnailUrl,

	@Schema(description = "가격", example = "850000")
	BigDecimal price,

	@Schema(description = "카테고리명", example = "전자기기")
	String categoryName,

	@Schema(description = "거래 위치", example = "서울특별시 강남구")
	String location,

	@Schema(description = "찜 수", example = "12")
	long favoriteCount,

	@Schema(description = "찜한 시각", example = "2026-05-02T12:00:00+09:00")
	OffsetDateTime likedAt
) {
}
