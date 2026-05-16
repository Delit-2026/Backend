package com.dealit.dealit.domain.wishlist.repository;

import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.wishlist.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

	Optional<Wishlist> findByMemberIdAndProductProductId(Long memberId, Long productId);

	Optional<Wishlist> findByMemberIdAndProductProductIdAndDeletedAtIsNull(Long memberId, Long productId);

	boolean existsByMemberIdAndProductProductIdAndDeletedAtIsNull(Long memberId, Long productId);

	long countByMemberIdAndDeletedAtIsNullAndProductDeletedAtIsNull(Long memberId);

	@EntityGraph(attributePaths = {"product", "product.images"})
	Page<Wishlist> findAllByMemberIdAndDeletedAtIsNullAndProductDeletedAtIsNull(Long memberId, Pageable pageable);

	@EntityGraph(attributePaths = {"product", "product.images"})
	Page<Wishlist> findAllByMemberIdAndProductSaleTypeAndDeletedAtIsNullAndProductDeletedAtIsNull(
		Long memberId,
		ProductSaleType saleType,
		Pageable pageable
	);
}
