package com.dealit.dealit.domain.payment.entity;

import com.dealit.dealit.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "product_payment",
	uniqueConstraints = @UniqueConstraint(name = "uk_product_payment_purchase_id", columnNames = "purchase_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "product_payment_seq_generator",
	sequenceName = "product_payment_seq",
	allocationSize = 1
)
public class ProductPayment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_payment_seq_generator")
	@Column(name = "product_payment_id")
	private Long productPaymentId;

	@Column(name = "purchase_id", nullable = false)
	private Long purchaseId;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "buyer_id", nullable = false)
	private Long buyerId;

	@Column(name = "seller_id", nullable = false)
	private Long sellerId;

	@Column(name = "amount", nullable = false)
	private Long amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private ProductPaymentStatus status;

	@Column(name = "paid_at", nullable = false)
	private LocalDateTime paidAt;

	@Column(name = "refunded_at")
	private LocalDateTime refundedAt;

	@Column(name = "settled_at")
	private LocalDateTime settledAt;

	@Column(name = "refund_reason", length = 100)
	private String refundReason;

	@Column(name = "settlement_reason", length = 100)
	private String settlementReason;

	private ProductPayment(Long purchaseId, Long productId, Long buyerId, Long sellerId, Long amount) {
		this.purchaseId = purchaseId;
		this.productId = productId;
		this.buyerId = buyerId;
		this.sellerId = sellerId;
		this.amount = amount;
		this.status = ProductPaymentStatus.HELD;
		this.paidAt = LocalDateTime.now();
	}

	public static ProductPayment held(Long purchaseId, Long productId, Long buyerId, Long sellerId, Long amount) {
		return new ProductPayment(purchaseId, productId, buyerId, sellerId, amount);
	}

	public void refund(String reason) {
		if (status != ProductPaymentStatus.HELD) {
			return;
		}
		status = ProductPaymentStatus.REFUNDED;
		refundedAt = LocalDateTime.now();
		refundReason = reason;
	}

	public void settle(String reason) {
		if (status != ProductPaymentStatus.HELD) {
			return;
		}
		status = ProductPaymentStatus.SETTLED;
		settledAt = LocalDateTime.now();
		settlementReason = reason;
	}
}
