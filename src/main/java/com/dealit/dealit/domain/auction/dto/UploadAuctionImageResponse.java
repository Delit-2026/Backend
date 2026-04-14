package com.dealit.dealit.domain.auction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 이미지 업로드 응답")
public record UploadAuctionImageResponse(
	@Schema(description = "업로드된 이미지 ID", example = "301")
	Long imageId,

	@Schema(description = "업로드된 이미지 URL", example = "http://localhost:8080/auction/images/301-sample.jpg")
	String imageUrl
) {
}
