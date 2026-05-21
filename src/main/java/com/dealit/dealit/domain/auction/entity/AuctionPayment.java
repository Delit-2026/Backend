package com.dealit.dealit.domain.auction.entity;

import com.dealit.dealit.domain.auction.AuctionPaymentStatus;
import com.dealit.dealit.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "auction_payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "auction_payment_seq_generator",
	sequenceName = "auction_payment_seq",
	allocationSize = 1
)
public class AuctionPayment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auction_payment_seq_generator")
	@Column(name = "auction_payment_id")
	private Long auctionPaymentId;

	@Column(name = "purchase_id")
	private Long purchaseId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "auction_id", nullable = false)
	private Auction auction;

	@Column(name = "bidder_id", nullable = false)
	private Long bidderId;

	@Column(name = "seller_id", nullable = false)
	private Long sellerId;

	@Column(name = "amount", nullable = false)
	private Long amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private AuctionPaymentStatus status;

	@Column(name = "reserved_at", nullable = false)
	private OffsetDateTime reservedAt;

	@Column(name = "refund_requested_at")
	private OffsetDateTime refundRequestedAt;

	@Column(name = "shipped_at")
	private OffsetDateTime shippedAt;

	@Column(name = "received_confirmed_at")
	private OffsetDateTime receivedConfirmedAt;

	@Column(name = "refunded_at")
	private OffsetDateTime refundedAt;

	@Column(name = "settled_at")
	private OffsetDateTime settledAt;

	@Column(name = "disputed_at")
	private OffsetDateTime disputedAt;

	private AuctionPayment(Auction auction, Long bidderId, Long sellerId, Long amount, OffsetDateTime reservedAt) {
		this.auction = auction;
		this.bidderId = bidderId;
		this.sellerId = sellerId;
		this.amount = amount;
		this.status = AuctionPaymentStatus.RESERVED;
		this.reservedAt = reservedAt;
	}

	public static AuctionPayment reserve(Auction auction, Long bidderId, Long sellerId, Long amount, OffsetDateTime reservedAt) {
		return new AuctionPayment(auction, bidderId, sellerId, amount, reservedAt);
	}

	public void linkPurchase(Long purchaseId) {
		if (this.purchaseId == null) {
			this.purchaseId = purchaseId;
		}
	}

	public boolean requestRefund(OffsetDateTime refundRequestedAt) {
		if (status == AuctionPaymentStatus.REFUND_PENDING || status == AuctionPaymentStatus.REFUNDED) {
			return false;
		}
		if (status != AuctionPaymentStatus.RESERVED) {
			return false;
		}
		this.status = AuctionPaymentStatus.REFUND_PENDING;
		this.refundRequestedAt = refundRequestedAt;
		return true;
	}

	public boolean completeRefund(OffsetDateTime refundedAt) {
		if (status == AuctionPaymentStatus.REFUNDED) {
			return false;
		}
		if (status != AuctionPaymentStatus.REFUND_PENDING) {
			return false;
		}
		this.status = AuctionPaymentStatus.REFUNDED;
		this.refundedAt = refundedAt;
		return true;
	}

	public boolean markShipped(OffsetDateTime shippedAt) {
		if (status != AuctionPaymentStatus.RESERVED) {
			return false;
		}
		this.status = AuctionPaymentStatus.SHIPPED;
		this.shippedAt = shippedAt;
		return true;
	}

	public boolean confirmReceived(OffsetDateTime receivedConfirmedAt) {
		if (status == AuctionPaymentStatus.SETTLED) {
			return false;
		}
		if (status != AuctionPaymentStatus.SHIPPED) {
			return false;
		}
		this.status = AuctionPaymentStatus.SETTLED;
		this.receivedConfirmedAt = receivedConfirmedAt;
		this.settledAt = receivedConfirmedAt;
		return true;
	}
}
