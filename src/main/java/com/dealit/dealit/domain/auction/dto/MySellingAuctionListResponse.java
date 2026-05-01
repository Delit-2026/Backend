package com.dealit.dealit.domain.auction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내 판매중 경매 목록 응답")
public record MySellingAuctionListResponse(
	@Schema(description = "판매중 경매 목록")
	List<MySellingAuctionItemResponse> content,

	@Schema(description = "현재 페이지 번호(0부터 시작)", example = "0")
	int page,

	@Schema(description = "페이지 크기", example = "20")
	int size,

	@Schema(description = "전체 상품 수", example = "1")
	long totalElements,

	@Schema(description = "다음 페이지 존재 여부", example = "false")
	boolean hasNext
) {
}
