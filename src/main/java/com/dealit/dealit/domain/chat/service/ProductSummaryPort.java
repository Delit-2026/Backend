package com.dealit.dealit.domain.chat.service;

public interface ProductSummaryPort {
    ProductSummary getSummaryByProductId(Long productId);

    record ProductSummary(
            Long productId,
            String name,
            String thumbnailUrl,
            String saleType,
            Long auctionId
    ) {
        public ProductSummary(Long productId, String name, String thumbnailUrl) {
            this(productId, name, thumbnailUrl, "REGULAR", null);
        }
    }
}
