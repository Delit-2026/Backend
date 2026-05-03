package com.dealit.dealit.domain.wishlist.entity;

import com.dealit.dealit.domain.product.entity.Product;
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
@Table(name = "wishlist")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "wishlist_seq_generator",
	sequenceName = "wishlist_seq",
	allocationSize = 1
)
public class Wishlist extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "wishlist_seq_generator")
	@Column(name = "wishlist_id")
	private Long wishlistId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	private Wishlist(Long memberId, Product product) {
		this.memberId = memberId;
		this.product = product;
	}

	public static Wishlist create(Long memberId, Product product) {
		return new Wishlist(memberId, product);
	}
}
