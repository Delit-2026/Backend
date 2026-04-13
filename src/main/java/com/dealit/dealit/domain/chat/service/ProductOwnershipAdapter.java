package com.dealit.dealit.domain.chat.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductOwnershipAdapter implements ProductOwnershipPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Long getOwnerIdByProductId(Long productId) {
        String sql = """
                SELECT seller_id
                FROM products
                WHERE product_id = :productId
                  AND deleted_at IS NULL
                """;

        Long ownerId = jdbcTemplate.query(
                sql,
                Map.of("productId", productId),
                rs -> rs.next() ? rs.getLong("seller_id") : null
        );

        if (ownerId == null) {
            throw new IllegalArgumentException("유효한 상품을 찾을 수 없습니다. productId=" + productId);
        }

        return ownerId;
    }
}