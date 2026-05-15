package com.dealit.dealit.domain.review.entity;

import com.dealit.dealit.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "review_seq_generator",
	sequenceName = "review_seq",
	allocationSize = 1
)
public class Review extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "review_seq_generator")
	@Column(name = "review_id")
	private Long reviewId;

	@Column(name = "reviewer_id", nullable = false)
	private Long reviewerId;

	@Column(name = "reviewee_id", nullable = false)
	private Long revieweeId;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "auction_id")
	private Long auctionId;

	@Column(name = "rating", nullable = false, precision = 2, scale = 1)
	private BigDecimal rating;

	@Column(name = "content", nullable = false, length = 1000)
	private String content;

	private Review(
		Long reviewerId,
		Long revieweeId,
		Long productId,
		Long auctionId,
		BigDecimal rating,
		String content
	) {
		this.reviewerId = reviewerId;
		this.revieweeId = revieweeId;
		this.productId = productId;
		this.auctionId = auctionId;
		this.rating = rating;
		this.content = content;
	}

	public static Review create(
		Long reviewerId,
		Long revieweeId,
		Long productId,
		Long auctionId,
		BigDecimal rating,
		String content
	) {
		return new Review(reviewerId, revieweeId, productId, auctionId, rating, content);
	}
}
