package com.dealit.dealit.domain.product.entity;

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
@Table(name = "product_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "product_image_seq_generator",
	sequenceName = "product_image_seq",
	allocationSize = 1
)
public class ProductImage extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_image_seq_generator")
	@Column(name = "image_id")
	private Long imageId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(name = "image_url", nullable = false, length = 500)
	private String imageUrl;

	@Column(name = "original_file_name", nullable = false, length = 255)
	private String originalFileName;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "sort_order")
	private Integer sortOrder;

	private ProductImage(String imageUrl, String originalFileName, Long memberId) {
		this.imageUrl = imageUrl;
		this.originalFileName = originalFileName;
		this.memberId = memberId;
	}

	public static ProductImage createTemporary(String imageUrl, String originalFileName, Long memberId) {
		return new ProductImage(imageUrl, originalFileName, memberId);
	}

	public void updateImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public void assignToProduct(Product product, int sortOrder) {
		this.product = product;
		this.sortOrder = sortOrder;
	}
}
