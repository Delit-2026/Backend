package com.dealit.dealit.domain.auction.dto;

import com.dealit.dealit.domain.auction.ProductSaleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "경매 상품 임시저장 요청")
public record SaveAuctionDraftRequest(
	@Schema(description = "상품명", example = "Apple Watch Series 9")
	@NotBlank(message = "name is required.")
	@Size(max = 100, message = "name must be 100 characters or fewer.")
	String name,

	@Schema(description = "상품 설명", example = "Box opened, lightly used, includes charger.")
	@NotBlank(message = "description is required.")
	@Size(max = 2000, message = "description must be 2000 characters or fewer.")
	String description,

	@Schema(description = "판매 유형", example = "AUCTION")
	@NotNull(message = "saleType is required.")
	ProductSaleType saleType,

	@Schema(description = "카테고리 ID", example = "101")
	@NotNull(message = "categoryId is required.")
	Long categoryId,

	@Schema(description = "일반 판매가", example = "120000", nullable = true)
	BigDecimal price,

	@Schema(description = "경매 시작가", example = "80000", nullable = true)
	BigDecimal startPrice,

	@Schema(description = "경매 종료 시각", example = "2026-04-15T12:00:00Z", nullable = true)
	OffsetDateTime auctionEndAt,

	@Schema(description = "가격 제안 허용 여부", example = "false")
	boolean allowOffer,

	@Schema(description = "상품 이미지 목록")
	@NotEmpty(message = "images must contain at least one image.")
	List<@Valid ProductImagePayload> images,

	@Schema(description = "거래 위치", example = "Seoul Gangnam-gu")
	@NotBlank(message = "location is required.")
	@Size(max = 100, message = "location must be 100 characters or fewer.")
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
