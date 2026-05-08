package com.dealit.dealit.domain.purchase.repository;

import com.dealit.dealit.domain.purchase.entity.Purchase;
import com.dealit.dealit.domain.purchase.entity.PurchaseStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

	Optional<Purchase> findByBuyerIdAndIdempotencyKey(Long buyerId, String idempotencyKey);

	List<Purchase> findTop100ByStatusAndShippingDeadlineAtLessThanEqualOrderByShippingDeadlineAtAsc(
		PurchaseStatus status,
		LocalDateTime now
	);

	List<Purchase> findTop100ByStatusAndAutoCompleteAtLessThanEqualOrderByAutoCompleteAtAsc(
		PurchaseStatus status,
		LocalDateTime now
	);
}
