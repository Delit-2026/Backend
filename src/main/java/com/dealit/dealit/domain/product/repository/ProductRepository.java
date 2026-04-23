package com.dealit.dealit.domain.product.repository;

import com.dealit.dealit.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
