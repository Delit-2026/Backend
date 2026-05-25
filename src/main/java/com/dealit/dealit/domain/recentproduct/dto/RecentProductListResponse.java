package com.dealit.dealit.domain.recentproduct.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "최근 본 상품 목록 응답")
public record RecentProductListResponse(
	@Schema(description = "최근 본 상품 목록")
	List<RecentProductItemResponse> content,

	@Schema(description = "조회 개수", example = "20")
	int size
) {
}
