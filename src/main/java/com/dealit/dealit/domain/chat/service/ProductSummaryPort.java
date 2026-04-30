package com.dealit.dealit.domain.chat.service;

public interface ProductSummaryPort {
    ProductSummary getSummaryByProductId(Long productId);

    record ProductSummary(
            Long productId,
            String name,
            String thumbnailUrl
    ) {}
}