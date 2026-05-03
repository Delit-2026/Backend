package com.dealit.dealit.domain.wallet.dto;

import com.dealit.dealit.domain.wallet.entity.WalletLedgerType;
import java.time.OffsetDateTime;

public record WalletLedgerResponse(
	Long ledgerId,
	WalletLedgerType type,
	Long amount,
	Long balanceAfter,
	String description,
	OffsetDateTime createdAt
) {
}
