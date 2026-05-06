package com.dealit.dealit.domain.purchase.repository;

import com.dealit.dealit.domain.purchase.entity.Purchase;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

	Optional<Purchase> findByBuyerIdAndIdempotencyKey(Long buyerId, String idempotencyKey);
}
