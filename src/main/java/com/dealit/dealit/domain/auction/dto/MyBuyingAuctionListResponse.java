package com.dealit.dealit.domain.auction.dto;

import java.util.List;

public record MyBuyingAuctionListResponse(
	List<MyBuyingAuctionItemResponse> content,
	int page,
	int size,
	long totalElements,
	boolean hasNext
) {
}
