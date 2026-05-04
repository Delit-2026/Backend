package com.dealit.dealit.domain.wallet.entity;

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
	name = "wallet",
	uniqueConstraints = @UniqueConstraint(name = "uk_wallet_member_id", columnNames = "member_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "wallet_seq_generator",
	sequenceName = "wallet_seq",
	allocationSize = 1
)
public class Wallet extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "wallet_seq_generator")
	@Column(name = "wallet_id")
	private Long walletId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "balance", nullable = false)
	private Long balance;

	private Wallet(Long memberId) {
		this.memberId = memberId;
		this.balance = 0L;
	}

	public static Wallet create(Long memberId) {
		return new Wallet(memberId);
	}

	public long apply(long amount) {
		long nextBalance = balance + amount;
		if (nextBalance < 0) {
			throw new IllegalArgumentException("잔액이 부족합니다.");
		}
		this.balance = nextBalance;
		return nextBalance;
	}
}
