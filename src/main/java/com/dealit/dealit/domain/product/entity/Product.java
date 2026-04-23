package com.dealit.dealit.domain.product.entity;

import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "product_seq_generator",
	sequenceName = "product_seq",
	allocationSize = 1
)
public class Product extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq_generator")
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

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "price", nullable = false, precision = 15, scale = 2)
	private BigDecimal price;

	@Column(name = "allow_offer", nullable = false)
	private boolean allowOffer;

	@Column(name = "location", nullable = false, length = 100)
	private String location;

	@Column(name = "draft_id")
	private Long draftId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private ProductStatus status;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
	private final List<ProductImage> images = new ArrayList<>();

	private Product(
		String name,
		String description,
		ProductSaleType saleType,
		Long categoryId,
		Long memberId,
		BigDecimal price,
		boolean allowOffer,
		String location,
		Long draftId,
		ProductStatus status
	) {
		this.name = name;
		this.description = description;
		this.saleType = saleType;
		this.categoryId = categoryId;
		this.memberId = memberId;
		this.price = price;
		this.allowOffer = allowOffer;
		this.location = location;
		this.draftId = draftId;
		this.status = status;
	}

	public static Product create(
		String name,
		String description,
		ProductSaleType saleType,
		Long categoryId,
		Long memberId,
		BigDecimal price,
		boolean allowOffer,
		String location,
		Long draftId,
		ProductStatus status
	) {
		return new Product(name, description, saleType, categoryId, memberId, price, allowOffer, location, draftId, status);
	}

	public void attachImage(ProductImage image, int sortOrder) {
		if (!images.contains(image)) {
			images.add(image);
		}
		image.assignToProduct(this, sortOrder);
	}
}
