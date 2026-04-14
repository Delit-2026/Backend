package com.dealit.dealit.domain.auction.dto;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.ProductSaleType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Objects;

@Schema(description = "경매 상품 등록 응답")
public record CreateAuctionResponse(
	@Schema(description = "등록된 상품 ID", example = "1001")
	Long productId,

	@Schema(description = "판매 유형", example = "AUCTION")
	ProductSaleType saleType,

	@Schema(description = "상품 상태", example = "AUCTION_LIVE")
	AuctionStatus status,

	@Schema(description = "경매 메타 정보")
	AuctionScheduleResponse auction
) {

	@Schema(description = "경매 일정 및 상태 정보")
	public record AuctionScheduleResponse(
		@Schema(description = "경매 상태", example = "AUCTION_SCHEDULED")
		AuctionStatus status,

		@Schema(description = "경매 시작 시각", example = "2026-04-15T10:00:00Z")
		OffsetDateTime startAt,

		@Schema(description = "경매 종료 시각", example = "2026-04-15T12:00:00Z")
		OffsetDateTime endAt
	) {
		public AuctionScheduleResponse {
			Objects.requireNonNull(status, "auction.status must not be null");
			Objects.requireNonNull(startAt, "auction.startAt must not be null");
			Objects.requireNonNull(endAt, "auction.endAt must not be null");
		}
	}
}
