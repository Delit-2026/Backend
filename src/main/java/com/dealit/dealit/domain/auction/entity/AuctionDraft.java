package com.dealit.dealit.domain.auction.entity;

import com.dealit.dealit.domain.auction.ProductSaleType;
import com.dealit.dealit.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "auction_draft")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "auction_draft_seq_generator",
	sequenceName = "auction_draft_seq",
	allocationSize = 1
)
public class AuctionDraft extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auction_draft_seq_generator")
	@Column(name = "draft_id")
	private Long draftId;

	@Column(name = "name", length = 100)
	private String name;

	@Column(name = "description", length = 2000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "sale_type", length = 20)
	private ProductSaleType saleType;

	@Column(name = "category_id")
	private Long categoryId;

	@Column(name = "price", precision = 15, scale = 2)
	private BigDecimal price;

	@Column(name = "start_price", precision = 15, scale = 2)
	private BigDecimal startPrice;

	@Column(name = "auction_end_at")
	private OffsetDateTime auctionEndAt;

	@Column(name = "allow_offer")
	private Boolean allowOffer;

	@Column(name = "location", length = 100)
	private String location;

	@Lob
	@Column(name = "payload_json", nullable = false)
	private String payloadJson;

	@Column(name = "saved_at", nullable = false)
	private OffsetDateTime savedAt;

	private AuctionDraft(
		String name,
		String description,
		ProductSaleType saleType,
		Long categoryId,
		BigDecimal price,
		BigDecimal startPrice,
		OffsetDateTime auctionEndAt,
		Boolean allowOffer,
		String location,
		String payloadJson,
		OffsetDateTime savedAt
	) {
		this.name = name;
		this.description = description;
		this.saleType = saleType;
		this.categoryId = categoryId;
		this.price = price;
		this.startPrice = startPrice;
		this.auctionEndAt = auctionEndAt;
		this.allowOffer = allowOffer;
		this.location = location;
		this.payloadJson = payloadJson;
		this.savedAt = savedAt;
	}

	public static AuctionDraft create(
		String name,
		String description,
		ProductSaleType saleType,
		Long categoryId,
		BigDecimal price,
		BigDecimal startPrice,
		OffsetDateTime auctionEndAt,
		Boolean allowOffer,
		String location,
		String payloadJson,
		OffsetDateTime savedAt
	) {
		return new AuctionDraft(
			name,
			description,
			saleType,
			categoryId,
			price,
			startPrice,
			auctionEndAt,
			allowOffer,
			location,
			payloadJson,
			savedAt
		);
	}

	public void update(
		String name,
		String description,
		ProductSaleType saleType,
		Long categoryId,
		BigDecimal price,
		BigDecimal startPrice,
		OffsetDateTime auctionEndAt,
		Boolean allowOffer,
		String location,
		String payloadJson,
		OffsetDateTime savedAt
	) {
		this.name = name;
		this.description = description;
		this.saleType = saleType;
		this.categoryId = categoryId;
		this.price = price;
		this.startPrice = startPrice;
		this.auctionEndAt = auctionEndAt;
		this.allowOffer = allowOffer;
		this.location = location;
		this.payloadJson = payloadJson;
		this.savedAt = savedAt;
	}
}
