package com.dealit.dealit.domain.auction.dto;

import com.dealit.dealit.domain.auction.AuctionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "내 판매중 경매 항목")
public record MySellingAuctionItemResponse(
	@Schema(description = "상품 ID", example = "10")
	Long productId,

	@Schema(description = "경매 ID. 별도 경매 테이블 도입 전까지 상품 ID와 동일", example = "10")
	Long auctionId,

	@Schema(description = "상품명", example = "아이폰 14 Pro")
	String name,

	@Schema(description = "상품 설명", example = "거의 새것입니다.")
	String description,

	@Schema(description = "카테고리명", example = "전자기기")
	String categoryName,

	@Schema(description = "썸네일 이미지 URL", example = "https://example.com/auction/images/10.jpg")
	String thumbnailUrl,

	@Schema(description = "경매 상태", example = "AUCTION_LIVE")
	AuctionStatus auctionStatus,

	@Schema(description = "경매 시작가", example = "700000")
	BigDecimal startPrice,

	@Schema(description = "현재가", example = "700000")
	BigDecimal currentPrice,

	@Schema(description = "최소 다음 입찰가", example = "700000")
	BigDecimal minimumNextBidPrice,

	@Schema(description = "입찰 수", example = "0")
	int bidCount,

	@Schema(description = "입찰자 수", example = "0")
	int bidderCount,

	@Schema(description = "경매 시작 시각", example = "2026-04-30T09:00:00+09:00")
	OffsetDateTime startAt,

	@Schema(description = "경매 종료 시각", example = "2026-05-03T09:00:00+09:00")
	OffsetDateTime endAt,

	@Schema(description = "수정 가능 여부", example = "true")
	boolean canEdit,

	@Schema(description = "삭제 가능 여부", example = "true")
	boolean canDelete,

	@Schema(description = "생성 시각", example = "2026-04-30T08:40:00+09:00")
	OffsetDateTime createdAt,

	@Schema(description = "수정 시각", example = "2026-04-30T10:12:00+09:00")
	OffsetDateTime updatedAt
) {
}
