package com.dealit.dealit.domain.auction.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BidResponse(
	Long auctionId,
	BigDecimal currentPrice,
	Long bidderId,
	OffsetDateTime serverTime,
	BidMessages messages
) {
	public record BidMessages(
		String reserveNotice,
		String refundNotice
	) {
		public static BidMessages defaults() {
			return new BidMessages(
				"입찰 시 해당 금액은 Dealit Pay에서 즉시 예치됩니다.",
				"경매에서 추월당할 경우 예치금은 자동 환불됩니다."
			);
		}
	}
}
