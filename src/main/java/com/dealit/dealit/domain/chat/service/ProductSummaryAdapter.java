package com.dealit.dealit.domain.chat.service;

import com.dealit.dealit.domain.chat.exception.ProductNotFoundException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductSummaryAdapter implements ProductSummaryPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public ProductSummary getSummaryByProductId(Long productId) {
        String sql = """
                SELECT product_id, name, thumbnail_url
                FROM products
                WHERE product_id = :productId
                  AND deleted_at IS NULL
                """;

        try {
            return jdbcTemplate.query(
                    sql,
                    Map.of("productId", productId),
                    rs -> {
                        if (!rs.next()) {
                            throw new ProductNotFoundException("유효한 상품을 찾을 수 없습니다. productId=" + productId);
                        }
                        return new ProductSummary(
                                rs.getLong("product_id"),
                                rs.getString("name"),
                                rs.getString("thumbnail_url")
                        );
                    }
            );
        } catch (DataAccessException e) {
            throw new ProductNotFoundException("유효한 상품을 찾을 수 없습니다. productId=" + productId);
        }
    }
}