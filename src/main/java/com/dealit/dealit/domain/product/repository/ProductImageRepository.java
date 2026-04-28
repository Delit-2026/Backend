package com.dealit.dealit.domain.product.repository;

import com.dealit.dealit.domain.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
	List<ProductImage> findAllByImageIdInAndDeletedAtIsNull(Collection<Long> imageIds);

	Optional<ProductImage> findByImageIdAndDeletedAtIsNull(Long imageId);
}
