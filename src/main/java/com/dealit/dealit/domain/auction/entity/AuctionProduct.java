package com.dealit.dealit.domain.auction.entity;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.ProductSaleType;
import com.dealit.dealit.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "auction_product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuctionProduct extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "product_id")
	private Long productId;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "description", nullable = false, length = 2000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "sale_type", nullable = false, length = 20)
	private ProductSaleType saleType;

	@Column(name = "category_id", nullable = false)
	private Long categoryId;

	@Column(name = "price", precision = 15, scale = 2)
	private BigDecimal price;

	@Column(name = "start_price", precision = 15, scale = 2)
	private BigDecimal startPrice;

	@Column(name = "auction_end_at")
	private OffsetDateTime auctionEndAt;

	@Column(name = "allow_offer", nullable = false)
	private boolean allowOffer;

	@Column(name = "location", nullable = false, length = 100)
	private String location;

	@Column(name = "draft_id")
	private Long draftId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private AuctionStatus status;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
	private final List<AuctionProductImage> images = new ArrayList<>();

	private AuctionProduct(
		String name,
		String description,
		ProductSaleType saleType,
		Long categoryId,
		BigDecimal price,
		BigDecimal startPrice,
		OffsetDateTime auctionEndAt,
		boolean allowOffer,
		String location,
		Long draftId,
		AuctionStatus status
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
		this.draftId = draftId;
		this.status = status;
	}

	public static AuctionProduct create(
		String name,
		String description,
		ProductSaleType saleType,
		Long categoryId,
		BigDecimal price,
		BigDecimal startPrice,
		OffsetDateTime auctionEndAt,
		boolean allowOffer,
		String location,
		Long draftId,
		AuctionStatus status
	) {
		return new AuctionProduct(
			name,
			description,
			saleType,
			categoryId,
			price,
			startPrice,
			auctionEndAt,
			allowOffer,
			location,
			draftId,
			status
		);
	}

	public void attachImage(AuctionProductImage image, int sortOrder) {
		if (!images.contains(image)) {
			images.add(image);
		}
		image.assignToProduct(this, sortOrder);
	}
}
