package com.dealit.dealit.domain.chat.service;

import com.dealit.dealit.domain.chat.exception.ProductNotFoundException;
import com.dealit.dealit.global.service.ImageUrlService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductSummaryAdapter implements ProductSummaryPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ImageUrlService imageUrlService;

    @Override
    public ProductSummary getSummaryByProductId(Long productId) {
        String sql = """
                SELECT
                    p.product_id,
                    p.name,
                    p.sale_type,
                    a.auction_id,
                    (
                        SELECT pi.image_url
                        FROM product_image pi
                        WHERE pi.product_id = p.product_id
                          AND pi.deleted_at IS NULL
                        ORDER BY pi.sort_order ASC, pi.image_id ASC
                        LIMIT 1
                    ) AS thumbnail_url
                FROM product p
                LEFT JOIN auction a
                  ON a.product_id = p.product_id
                 AND a.deleted_at IS NULL
                WHERE p.product_id = :productId
                  AND p.deleted_at IS NULL
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
                                toPublicImageUrl(rs.getString("thumbnail_url")),
                                rs.getString("sale_type"),
                                rs.getObject("auction_id", Long.class)
                        );
                    }
            );
        } catch (DataAccessException e) {
            throw new ProductNotFoundException("유효한 상품을 찾을 수 없습니다. productId=" + productId);
        }
    }

    private String toPublicImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        return imageUrlService.toPublicUrl(imageUrl);
    }
}
