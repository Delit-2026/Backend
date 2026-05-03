package com.dealit.dealit.domain.wishlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내 찜 목록 응답")
public record MyWishlistListResponse(
	@Schema(description = "찜 목록")
	List<MyWishlistItemResponse> content,

	@Schema(description = "현재 페이지 번호(0부터 시작)", example = "0")
	int page,

	@Schema(description = "페이지 크기", example = "20")
	int size,

	@Schema(description = "전체 찜 수", example = "1")
	long totalElements,

	@Schema(description = "다음 페이지 존재 여부", example = "false")
	boolean hasNext
) {
}
