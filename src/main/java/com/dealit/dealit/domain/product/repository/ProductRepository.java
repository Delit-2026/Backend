package com.dealit.dealit.domain.product.repository;

import com.dealit.dealit.domain.product.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "images")
    Optional<Product> findByProductIdAndDeletedAtIsNull(Long productId);
}