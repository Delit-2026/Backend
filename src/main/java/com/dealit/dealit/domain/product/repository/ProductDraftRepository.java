package com.dealit.dealit.domain.product.repository;

import com.dealit.dealit.domain.product.entity.ProductDraft;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductDraftRepository extends JpaRepository<ProductDraft, Long> {
}
