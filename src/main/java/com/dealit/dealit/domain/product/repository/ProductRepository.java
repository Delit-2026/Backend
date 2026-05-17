package com.dealit.dealit.domain.product.repository;

import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import com.dealit.dealit.domain.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
	List<Product> findAllByMemberIdAndDeletedAtIsNullAndStatusOrderByCreatedAtDesc(Long memberId, ProductStatus status);

	List<Product> findAllByMemberIdAndDeletedAtIsNullAndStatusInOrderByCreatedAtDesc(Long memberId, Collection<ProductStatus> statuses);

	@EntityGraph(attributePaths = {"images"})
	Optional<Product> findByProductIdAndDeletedAtIsNull(Long productId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Product> findWithLockByProductIdAndDeletedAtIsNull(Long productId);

	@EntityGraph(attributePaths = {"images"})
	Page<Product> findAllByMemberIdAndStatusAndDeletedAtIsNull(
		Long memberId,
		ProductStatus status,
		Pageable pageable
	);

	@EntityGraph(attributePaths = {"images"})
	Optional<Product> findByProductIdAndMemberIdAndDeletedAtIsNull(Long productId, Long memberId);

	@EntityGraph(attributePaths = {"images"})
	List<Product> findAllByProductIdInAndDeletedAtIsNull(Collection<Long> productIds);

	@EntityGraph(attributePaths = {"images"})
	List<Product> findAllBySaleTypeAndStatusAndDeletedAtIsNull(ProductSaleType saleType, ProductStatus status);
}
