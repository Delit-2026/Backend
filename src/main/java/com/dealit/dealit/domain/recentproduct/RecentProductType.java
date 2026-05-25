package com.dealit.dealit.domain.recentproduct;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "최근 본 상품 유형")
public enum RecentProductType {
	REGULAR,
	AUCTION
}
