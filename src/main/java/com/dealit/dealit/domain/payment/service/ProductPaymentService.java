package com.dealit.dealit.domain.payment.service;

import com.dealit.dealit.domain.payment.entity.ProductPayment;
import com.dealit.dealit.domain.payment.repository.ProductPaymentRepository;
import com.dealit.dealit.domain.wallet.service.WalletService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductPaymentService {

	private final ProductPaymentRepository productPaymentRepository;
	private final WalletService walletService;

	public ProductPayment hold(Long purchaseId, Long productId, Long buyerId, Long sellerId, long amount) {
		return productPaymentRepository.save(ProductPayment.held(purchaseId, productId, buyerId, sellerId, amount));
	}

	public void refund(ProductPayment payment, String reason) {
		if (payment == null || payment.getRefundedAt() != null || payment.getSettledAt() != null) {
			return;
		}
		walletService.refund(payment.getBuyerId(), payment.getAmount());
		payment.refund(reason);
	}

	public void settle(ProductPayment payment, String reason) {
		if (payment == null || payment.getSettledAt() != null || payment.getRefundedAt() != null) {
			return;
		}
		walletService.settlePurchase(payment.getSellerId(), payment.getAmount(), payment.getPurchaseId());
		payment.settle(reason);
	}

	public ProductPayment getByPurchaseId(Long purchaseId) {
		return productPaymentRepository.findByPurchaseId(purchaseId)
			.orElseThrow(() -> new IllegalStateException("결제 예치 정보를 찾을 수 없습니다."));
	}

	public Optional<ProductPayment> findByPurchaseId(Long purchaseId) {
		return productPaymentRepository.findByPurchaseId(purchaseId);
	}
}
