package com.dealit.dealit.domain.search.dto;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.product.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "통합 검색 결과 항목")
public record SearchItemResponse(
	@Schema(description = "검색 문서 ID", example = "REGULAR-1")
	String id,

	@Schema(description = "검색 결과 유형", example = "REGULAR")
	SearchResultType type,

	@Schema(description = "상품 ID", example = "1")
	Long productId,

	@Schema(description = "경매 ID", example = "10")
	Long auctionId,

	@Schema(description = "상품명", example = "맥북 에어")
	String name,

	@Schema(description = "상품 설명", example = "상태 좋은 맥북입니다.")
	String description,

	@Schema(description = "대표 이미지 URL", example = "http://localhost:8080/uploads/product/images/example.jpg")
	String thumbnailUrl,

	@Schema(description = "최하위 카테고리 ID", example = "123")
	Long categoryId,

	@Schema(description = "카테고리 경로 ID 목록", example = "[1, 10, 123]")
	List<Long> categoryPathIds,

	@Schema(description = "카테고리 경로명 목록", example = "[\"전자기기\", \"노트북\", \"맥북\"]")
	List<String> categoryNames,

	@Schema(description = "일반 상품 가격", example = "30000")
	BigDecimal price,

	@Schema(description = "경매 현재가", example = "50000")
	BigDecimal currentPrice,

	@Schema(description = "거래 위치", example = "서울 강남구")
	String location,

	@Schema(description = "상품 상태", example = "ON_SALE")
	ProductStatus productStatus,

	@Schema(description = "경매 상태", example = "ONGOING")
	AuctionStatus auctionStatus,

	@Schema(description = "경매 종료 시각", example = "2026-05-18T18:00:00+09:00")
	OffsetDateTime endsAt,

	@Schema(description = "조회수", example = "120")
	long viewCount,

	@Schema(description = "찜 수", example = "15")
	long favoriteCount,

	@Schema(description = "등록 시각", example = "2026-05-17T12:00:00+09:00")
	OffsetDateTime createdAt
) {
}
