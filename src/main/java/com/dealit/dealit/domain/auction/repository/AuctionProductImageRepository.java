package com.dealit.dealit.domain.auction.repository;

import com.dealit.dealit.domain.auction.entity.AuctionProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AuctionProductImageRepository extends JpaRepository<AuctionProductImage, Long> {

	List<AuctionProductImage> findAllByImageIdInAndDeletedAtIsNull(Collection<Long> imageIds);

	Optional<AuctionProductImage> findByImageIdAndDeletedAtIsNull(Long imageId);
}
