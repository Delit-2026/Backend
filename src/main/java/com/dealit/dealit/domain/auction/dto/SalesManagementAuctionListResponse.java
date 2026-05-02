package com.dealit.dealit.domain.auction.dto;

import java.util.List;

public record SalesManagementAuctionListResponse(
	List<SalesManagementAuctionProductResponse> items
) {
}
