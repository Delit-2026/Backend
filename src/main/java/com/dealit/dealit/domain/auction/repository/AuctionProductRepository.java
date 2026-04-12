package com.dealit.dealit.domain.auction.repository;

import com.dealit.dealit.domain.auction.entity.AuctionProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionProductRepository extends JpaRepository<AuctionProduct, Long> {
}
