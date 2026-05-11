package com.dealit.dealit.domain.purchase.entity;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "purchase",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_purchase_buyer_idempotency",
		columnNames = {"buyer_id", "idempotency_key"}
	)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "purchase_seq_generator",
	sequenceName = "purchase_seq",
	allocationSize = 1
)
public class Purchase extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "purchase_seq_generator")
	@Column(name = "purchase_id")
	private Long purchaseId;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "buyer_id", nullable = false)
	private Long buyerId;

	@Column(name = "seller_id", nullable = false)
	private Long sellerId;

	@Column(name = "price_snapshot", nullable = false, precision = 15, scale = 2)
	private BigDecimal priceSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private PurchaseStatus status;

	@Column(name = "idempotency_key", nullable = false, length = 100)
	private String idempotencyKey;

	@Column(name = "chat_room_id")
	private Long chatRoomId;

	@Column(name = "purchased_at")
	private LocalDateTime purchasedAt;

	@Column(name = "shipping_deadline_at")
	private LocalDateTime shippingDeadlineAt;

	@Column(name = "shipped_at")
	private LocalDateTime shippedAt;

	@Column(name = "auto_complete_at")
	private LocalDateTime autoCompleteAt;

	@Column(name = "buyer_completed_at")
	private LocalDateTime buyerCompletedAt;

	@Column(name = "seller_completed_at")
	private LocalDateTime sellerCompletedAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;

	@Column(name = "settled_at")
	private LocalDateTime settledAt;

	private Purchase(
		Long productId,
		Long buyerId,
		Long sellerId,
		BigDecimal priceSnapshot,
		String idempotencyKey
	) {
		this.productId = productId;
		this.buyerId = buyerId;
		this.sellerId = sellerId;
		this.priceSnapshot = priceSnapshot;
		this.idempotencyKey = idempotencyKey;
		this.status = PurchaseStatus.PAID;
		this.purchasedAt = LocalDateTime.now();
		this.shippingDeadlineAt = this.purchasedAt.plusDays(3);
	}

	public static Purchase paid(
		Long productId,
		Long buyerId,
		Long sellerId,
		BigDecimal priceSnapshot,
		String idempotencyKey
	) {
		return new Purchase(productId, buyerId, sellerId, priceSnapshot, idempotencyKey);
	}

	public void linkChatRoom(Long chatRoomId) {
		this.chatRoomId = chatRoomId;
	}

	public void markShipped() {
		if ((status == PurchaseStatus.SHIPPED || status == PurchaseStatus.COMPLETED) && shippedAt != null) {
			return;
		}
		if (status != PurchaseStatus.PAID) {
			throw new IllegalStateException("발송 완료 처리할 수 없는 거래 상태입니다.");
		}
		status = PurchaseStatus.SHIPPED;
		shippedAt = LocalDateTime.now();
		sellerCompletedAt = shippedAt;
		autoCompleteAt = shippedAt.plusDays(7);
	}

	public void markBuyerCompleted() {
		if (status != PurchaseStatus.SHIPPED && status != PurchaseStatus.COMPLETED) {
			throw new IllegalStateException("판매자 발송 완료 후 수령 확인할 수 있습니다.");
		}
		if (buyerCompletedAt == null) {
			buyerCompletedAt = LocalDateTime.now();
		}
	}

	public void markSellerCompleted() {
		if (sellerCompletedAt == null) {
			sellerCompletedAt = LocalDateTime.now();
		}
	}

	public boolean isBothCompleted() {
		return buyerCompletedAt != null && sellerCompletedAt != null;
	}

	public void complete() {
		if (status == PurchaseStatus.COMPLETED) {
			return;
		}
		status = PurchaseStatus.COMPLETED;
		completedAt = LocalDateTime.now();
	}

	public void cancel() {
		if (status != PurchaseStatus.PAID) {
			throw new IllegalStateException("취소할 수 없는 거래 상태입니다.");
		}
		status = PurchaseStatus.CANCELED;
		canceledAt = LocalDateTime.now();
	}

	public boolean isSettled() {
		return settledAt != null;
	}

	public void settle() {
		if (settledAt == null) {
			settledAt = LocalDateTime.now();
		}
	}
}
