package com.dealit.dealit.domain.product.repository;

import com.dealit.dealit.domain.product.ProductStatus;
import com.dealit.dealit.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
	List<Product> findAllByMemberIdAndDeletedAtIsNullAndStatusOrderByCreatedAtDesc(Long memberId, ProductStatus status);

	List<Product> findAllByMemberIdAndDeletedAtIsNullAndStatusInOrderByCreatedAtDesc(Long memberId, Collection<ProductStatus> statuses);

	Optional<Product> findByProductIdAndDeletedAtIsNull(Long productId);
}
