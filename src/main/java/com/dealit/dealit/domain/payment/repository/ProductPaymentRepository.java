package com.dealit.dealit.domain.payment.repository;

import com.dealit.dealit.domain.payment.entity.ProductPayment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductPaymentRepository extends JpaRepository<ProductPayment, Long> {

	Optional<ProductPayment> findByPurchaseId(Long purchaseId);
}
