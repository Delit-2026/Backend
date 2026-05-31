package com.dealit.dealit.domain.product.dto;

import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "일반 상품 상세 조회 응답")
public record ProductDetailResponse(
	@Schema(description = "상품 ID", example = "1001")
	Long productId,

	@Schema(description = "상품명", example = "아이폰 14 Pro")
	String name,

	@Schema(description = "상품 설명", example = "거의 새 상품입니다.")
	String description,

	@Schema(description = "판매 유형", example = "REGULAR", allowableValues = {"REGULAR"})
	ProductSaleType saleType,

	@Schema(description = "카테고리 정보")
	CategoryResponse category,

	@Schema(description = "상품 이미지 URL 목록")
	List<String> imageUrls,

	@Schema(description = "상품 상태", example = "ON_SALE")
	ProductStatus status,

	@Schema(description = "판매자 정보")
	SellerResponse seller,

	@Schema(description = "생성 시각", example = "2026-05-02T10:30:00+09:00")
	OffsetDateTime createdAt,

	@Schema(description = "수정 시각", example = "2026-05-02T11:00:00+09:00")
	OffsetDateTime updatedAt,

	@Schema(description = "일반 판매 상세 정보")
	GeneralSaleResponse generalSale,

	@Schema(description = "채팅 가능 여부", example = "true")
	boolean canChat,

	@Schema(description = "구매 가능 여부", example = "true")
	boolean canPurchase,

	@Schema(description = "구매 불가 사유", example = "EMAIL_NOT_VERIFIED", nullable = true)
	String purchaseBlockedReason,

	@Schema(description = "찜 가능 여부", example = "true")
	boolean canFavorite
) {

	public record CategoryResponse(
		@Schema(description = "카테고리 ID", example = "19")
		Long categoryId,

		@Schema(description = "카테고리 한글명", example = "전자기기")
		String nameKo,

		@Schema(description = "카테고리 영문명", example = "Digital/Electronics")
		String nameEn
	) {
	}

	public record SellerResponse(
		@Schema(description = "판매자 회원 ID", example = "3")
		Long memberId,

		@Schema(description = "판매자 닉네임", example = "dealit-user-3")
		String nickname,

		@Schema(description = "판매자 프로필 이미지 URL", example = "https://api.dealit.site/uploads/profile/images/3-profile.png", nullable = true)
		String profileImageUrl,

		@Schema(description = "판매자 소개글", example = "Good seller.", nullable = true)
		String bio,

		@Schema(description = "거래 위치", example = "서울 강남구")
		String location,

		@Schema(description = "판매자 평균 평점", example = "4.5")
		double rating
	) {
	}

	public record GeneralSaleResponse(
		@Schema(description = "일반 판매 가격", example = "12000")
		BigDecimal price,

		@Schema(description = "조회수", example = "10")
		long viewCount,

		@Schema(description = "찜 수", example = "2")
		long favoriteCount,

		@Schema(description = "채팅 수", example = "1")
		long chatCount,

		@Schema(description = "일반 판매 상태", example = "ON_SALE")
		ProductStatus status
	) {
	}
}
