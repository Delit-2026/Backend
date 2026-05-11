package com.dealit.dealit.domain.auction.entity;

import com.dealit.dealit.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "buying_auction_hidden",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_buying_auction_hidden_member_auction",
		columnNames = {"member_id", "auction_id"}
	)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "buying_auction_hidden_seq_generator",
	sequenceName = "buying_auction_hidden_seq",
	allocationSize = 1
)
public class BuyingAuctionHidden extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "buying_auction_hidden_seq_generator")
	@Column(name = "hidden_id")
	private Long hiddenId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "auction_id", nullable = false)
	private Long auctionId;

	private BuyingAuctionHidden(Long memberId, Long auctionId) {
		this.memberId = memberId;
		this.auctionId = auctionId;
	}

	public static BuyingAuctionHidden create(Long memberId, Long auctionId) {
		return new BuyingAuctionHidden(memberId, auctionId);
	}
}
