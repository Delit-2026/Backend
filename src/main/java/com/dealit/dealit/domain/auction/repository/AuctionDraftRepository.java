package com.dealit.dealit.domain.auction.repository;

import com.dealit.dealit.domain.auction.entity.AuctionDraft;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionDraftRepository extends JpaRepository<AuctionDraft, Long> {
}
