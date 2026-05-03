package com.dealit.dealit.domain.auction.entity;

import com.dealit.dealit.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "bid")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "bid_seq_generator",
	sequenceName = "bid_seq",
	allocationSize = 1
)
public class Bid extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bid_seq_generator")
	@Column(name = "bid_id")
	private Long bidId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "auction_id", nullable = false)
	private Auction auction;

	@Column(name = "bidder_id", nullable = false)
	private Long bidderId;

	@Column(name = "bid_price", nullable = false, precision = 15, scale = 2)
	private BigDecimal bidPrice;

	private Bid(Auction auction, Long bidderId, BigDecimal bidPrice) {
		this.auction = auction;
		this.bidderId = bidderId;
		this.bidPrice = bidPrice;
	}

	public static Bid create(Auction auction, Long bidderId, BigDecimal bidPrice) {
		return new Bid(auction, bidderId, bidPrice);
	}
}
