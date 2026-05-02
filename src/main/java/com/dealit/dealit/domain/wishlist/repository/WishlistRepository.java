package com.dealit.dealit.domain.wishlist.repository;

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

	long countByMemberIdAndDeletedAtIsNull(Long memberId);

	@EntityGraph(attributePaths = {"product", "product.images"})
	Page<Wishlist> findAllByMemberIdAndDeletedAtIsNull(Long memberId, Pageable pageable);
}
