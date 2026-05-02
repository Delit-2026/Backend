package com.dealit.dealit.domain.auction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Schema(description = "경매 입찰 요청")
public record BidRequest(
	@Schema(description = "입찰가", example = "12000")
	@NotNull(message = "입찰가는 필수입니다.")
	@Positive(message = "입찰가는 0보다 커야 합니다.")
	BigDecimal bidPrice
) {
}
