package com.dealit.dealit.domain.auction.dto;

import com.dealit.dealit.domain.auction.AuctionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "경매 수정용 상세 응답")
public record AuctionEditDetailResponse(
	@Schema(description = "상품 ID", example = "10")
	Long productId,

	@Schema(description = "경매 ID. 별도 경매 테이블 도입 전까지 상품 ID와 동일", example = "10")
	Long auctionId,

	@Schema(description = "상품명", example = "아이폰 14 Pro")
	String name,

	@Schema(description = "상품 설명", example = "거의 새것입니다.")
	String description,

	@Schema(description = "카테고리 ID", example = "19")
	Long categoryId,

	@Schema(description = "카테고리명", example = "전자기기")
	String categoryName,

	@Schema(description = "거래 위치", example = "서울 강남구")
	String location,

	@Schema(description = "상품 이미지 목록")
	List<AuctionEditImageResponse> images,

	@Schema(description = "경매 시작가", example = "700000")
	BigDecimal startPrice,

	@Schema(description = "경매 진행 기간(일)", example = "3")
	long auctionDurationDays,

	@Schema(description = "경매 상태", example = "AUCTION_LIVE")
	AuctionStatus auctionStatus,

	@Schema(description = "입찰 수", example = "0")
	int bidCount,

	@Schema(description = "입찰자 수", example = "0")
	int bidderCount,

	@Schema(description = "수정 가능 여부", example = "true")
	boolean canEdit
) {

	@Schema(description = "경매 수정용 이미지")
	public record AuctionEditImageResponse(
		@Schema(description = "이미지 ID", example = "1")
		Long imageId,

		@Schema(description = "이미지 URL", example = "https://example.com/auction/images/10.jpg")
		String imageUrl,

		@Schema(description = "정렬 순서", example = "1")
		Integer sortOrder
	) {
	}
}
