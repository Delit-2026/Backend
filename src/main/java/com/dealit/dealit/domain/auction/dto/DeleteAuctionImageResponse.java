package com.dealit.dealit.domain.auction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 이미지 삭제 응답")
public record DeleteAuctionImageResponse(
	@Schema(description = "삭제된 이미지 ID", example = "5")
	Long imageId,

	@Schema(description = "삭제 성공 여부", example = "true")
	boolean deleted
) {
}
