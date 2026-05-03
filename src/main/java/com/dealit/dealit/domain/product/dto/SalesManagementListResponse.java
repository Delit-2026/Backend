package com.dealit.dealit.domain.product.dto;

import java.util.List;

public record SalesManagementListResponse(
	List<SalesManagementProductResponse> items
) {
}
