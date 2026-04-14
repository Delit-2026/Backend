package com.dealit.dealit.domain.chat.service;

import com.dealit.dealit.domain.chat.exception.ProductNotFoundException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
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

        try {
            Long ownerId = jdbcTemplate.query(
                    sql,
                    Map.of("productId", productId),
                    rs -> rs.next() ? rs.getLong("seller_id") : null
            );

            if (ownerId == null) {
                throw new ProductNotFoundException("유효한 상품을 찾을 수 없습니다. productId=" + productId);
            }
            return ownerId;
        } catch (DataAccessException e) {
            throw new ProductNotFoundException("유효한 상품을 찾을 수 없습니다. productId=" + productId);
        }
    }
}