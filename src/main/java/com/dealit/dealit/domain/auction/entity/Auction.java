package com.dealit.dealit.domain.auction.entity;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.product.entity.Product;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "auction")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "auction_seq_generator",
	sequenceName = "auction_seq",
	allocationSize = 1
)
public class Auction extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auction_seq_generator")
	@Column(name = "auction_id")
	private Long auctionId;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "start_price", nullable = false, precision = 15, scale = 2)
	private BigDecimal startPrice;

	@Column(name = "current_price", nullable = false, precision = 15, scale = 2)
	private BigDecimal currentPrice;

	@Column(name = "auction_start_at", nullable = false)
	private OffsetDateTime auctionStartAt;

	@Column(name = "auction_end_at", nullable = false)
	private OffsetDateTime auctionEndAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private AuctionStatus status;

	private Auction(
		Product product,
		BigDecimal startPrice,
		BigDecimal currentPrice,
		OffsetDateTime auctionStartAt,
		OffsetDateTime auctionEndAt,
		AuctionStatus status
	) {
		this.product = product;
		this.startPrice = startPrice;
		this.currentPrice = currentPrice;
		this.auctionStartAt = auctionStartAt;
		this.auctionEndAt = auctionEndAt;
		this.status = status;
	}

	public static Auction create(
		Product product,
		BigDecimal startPrice,
		OffsetDateTime auctionStartAt,
		OffsetDateTime auctionEndAt,
		AuctionStatus status
	) {
		return new Auction(product, startPrice, startPrice, auctionStartAt, auctionEndAt, status);
	}

	public void updateEditableDetails(BigDecimal startPrice, OffsetDateTime auctionEndAt) {
		this.startPrice = startPrice;
		this.currentPrice = startPrice;
		this.auctionEndAt = auctionEndAt;
	}
}
