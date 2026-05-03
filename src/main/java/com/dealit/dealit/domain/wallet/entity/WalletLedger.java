package com.dealit.dealit.domain.wallet.entity;

import com.dealit.dealit.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "wallet_ledger")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "wallet_ledger_seq_generator",
	sequenceName = "wallet_ledger_seq",
	allocationSize = 1
)
public class WalletLedger extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "wallet_ledger_seq_generator")
	@Column(name = "wallet_ledger_id")
	private Long walletLedgerId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "wallet_id", nullable = false)
	private Wallet wallet;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 30)
	private WalletLedgerType type;

	@Column(name = "amount", nullable = false)
	private Long amount;

	@Column(name = "balance_after", nullable = false)
	private Long balanceAfter;

	@Column(name = "description", nullable = false, length = 100)
	private String description;

	private WalletLedger(
		Wallet wallet,
		WalletLedgerType type,
		long amount,
		long balanceAfter,
		String description
	) {
		this.wallet = wallet;
		this.memberId = wallet.getMemberId();
		this.type = type;
		this.amount = amount;
		this.balanceAfter = balanceAfter;
		this.description = description;
	}

	public static WalletLedger create(
		Wallet wallet,
		WalletLedgerType type,
		long amount,
		long balanceAfter,
		String description
	) {
		return new WalletLedger(wallet, type, amount, balanceAfter, description);
	}
}
