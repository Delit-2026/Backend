package com.dealit.dealit.domain.auction.dto;

import com.dealit.dealit.domain.auction.ProductSaleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "경매 상품 임시저장 요청")
public record SaveAuctionDraftRequest(
	@Schema(description = "상품명", example = "애플워치 시리즈 9")
	@Size(max = 100, message = "상품명은 100자 이하여야 합니다.")
	String name,

	@Schema(description = "상품 설명", example = "개봉 후 몇 번 사용하지 않았고 충전기도 함께 드립니다.")
	@Size(max = 2000, message = "상품 설명은 2000자 이하여야 합니다.")
	String description,

	@Schema(description = "판매 유형", example = "AUCTION")
	ProductSaleType saleType,

	@Schema(description = "카테고리 ID", example = "101")
	Long categoryId,

	@Schema(description = "일반 판매가", example = "120000", nullable = true)
	BigDecimal price,

	@Schema(description = "경매 시작가", example = "80000", nullable = true)
	BigDecimal startPrice,

	@Schema(description = "경매 종료 시각", example = "2026-04-15T12:00:00Z", nullable = true)
	OffsetDateTime auctionEndAt,

	@Schema(description = "가격 제안 허용 여부", example = "false", nullable = true)
	Boolean allowOffer,

	@Schema(description = "상품 이미지 목록", nullable = true)
	List<@Valid ProductImagePayload> images,

	@Schema(description = "거래 위치", example = "서울 강남구")
	@Size(max = 100, message = "거래 위치는 100자 이하여야 합니다.")
	String location,

	@Schema(description = "임시저장 ID", example = "5", nullable = true)
	Long draftId
) {
	public SaveAuctionDraftRequest(CreateAuctionRequest request) {
		this(
			request.name(),
			request.description(),
			request.saleType(),
			request.categoryId(),
			request.price(),
			request.startPrice(),
			request.auctionEndAt(),
			request.allowOffer(),
			request.images(),
			request.location(),
			request.draftId()
		);
	}
}
