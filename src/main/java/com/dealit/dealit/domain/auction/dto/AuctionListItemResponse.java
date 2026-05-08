package com.dealit.dealit.domain.auction.dto;

import com.dealit.dealit.domain.auction.AuctionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "경매 상품 목록 항목")
public record AuctionListItemResponse(
	@Schema(description = "경매 ID", example = "1")
	Long auctionId,

	@Schema(description = "상품 ID", example = "1")
	Long productId,

	@Schema(description = "상품명", example = "맥북 에어 경매")
	String title,

	@Schema(description = "대표 이미지 URL", example = "http://localhost:8080/uploads/auction/images/1.jpg")
	String thumbnailUrl,

	@Schema(description = "시작가", example = "10000")
	BigDecimal startPrice,

	@Schema(description = "현재 입찰가", example = "35000")
	BigDecimal currentPrice,

	@Schema(description = "입찰 수", example = "12")
	long bidCount,

	@Schema(description = "경매 종료 시각", example = "2026-05-06T18:00:00+09:00")
	OffsetDateTime endAt,

	@Schema(description = "경매 상태", example = "ONGOING")
	AuctionStatus auctionStatus,

	@Schema(description = "거래 위치", example = "서울 강남구")
	String location,

	@Schema(description = "카테고리명", example = "디지털기기")
	String categoryName,

	@Schema(description = "판매자 회원 ID", example = "10")
	Long sellerId,

	@Schema(description = "경매 인기 점수", example = "4.5")
	double popularScore,

	@Schema(description = "순위", example = "1")
	int rank,

	@Schema(description = "등록 시각", example = "2026-05-03T12:00:00+09:00")
	OffsetDateTime createdAt
) {
}
