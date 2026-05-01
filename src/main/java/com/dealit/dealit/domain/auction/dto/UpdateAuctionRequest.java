package com.dealit.dealit.domain.auction.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "경매 상품 수정 요청")
public record UpdateAuctionRequest(
	@Schema(description = "상품명", example = "맥북에어입니다")
	@NotBlank(message = "상품명은 필수입니다.")
	@Size(max = 100, message = "상품명은 100자 이하여야 합니다.")
	String name,

	@Schema(description = "상품 설명", example = "맥북입니다!!! 에어이고 많이안써서 깨끗해용")
	@NotBlank(message = "상품 설명은 필수입니다.")
	@Size(max = 2000, message = "상품 설명은 2000자 이하여야 합니다.")
	String description,

	@Schema(description = "카테고리 ID", example = "19")
	@NotNull(message = "카테고리 ID는 필수입니다.")
	Long categoryId,

	@Schema(description = "경매 시작가", example = "400000")
	@NotNull(message = "경매 시작가는 필수입니다.")
	BigDecimal startPrice,

	@Schema(description = "경매 진행 기간(일 단위)", example = "1")
	@NotNull(message = "경매 진행 기간은 필수입니다.")
	@Positive(message = "경매 진행 기간은 0보다 커야 합니다.")
	Integer auctionDurationDays,

	@Schema(description = "거래 위치", example = "경기도 부천시 소사구 소사동로72번길 32")
	@NotBlank(message = "거래 위치는 필수입니다.")
	@Size(max = 100, message = "거래 위치는 100자 이하여야 합니다.")
	String location,

	@ArraySchema(schema = @Schema(implementation = ProductImagePayload.class), minItems = 1)
	@NotEmpty(message = "이미지는 최소 1장 이상 등록해야 합니다.")
	List<@Valid ProductImagePayload> images
) {
}
