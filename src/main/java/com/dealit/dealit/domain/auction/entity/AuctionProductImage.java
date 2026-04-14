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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "auction_product_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "auction_product_image_seq_generator",
	sequenceName = "auction_product_image_seq",
	allocationSize = 1
)
public class AuctionProductImage extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auction_product_image_seq_generator")
	@Column(name = "image_id")
	private Long imageId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private AuctionProduct product;

	@Column(name = "image_url", nullable = false, length = 500)
	private String imageUrl;

	@Column(name = "original_file_name", nullable = false, length = 255)
	private String originalFileName;

	@Column(name = "sort_order")
	private Integer sortOrder;

	private AuctionProductImage(String imageUrl, String originalFileName) {
		this.imageUrl = imageUrl;
		this.originalFileName = originalFileName;
	}

	public static AuctionProductImage createTemporary(String imageUrl, String originalFileName) {
		return new AuctionProductImage(imageUrl, originalFileName);
	}

	public void updateImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public void assignToProduct(AuctionProduct product, int sortOrder) {
		this.product = product;
		this.sortOrder = sortOrder;
	}
}
