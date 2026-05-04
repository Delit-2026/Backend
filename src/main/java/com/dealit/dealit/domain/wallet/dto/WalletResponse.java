package com.dealit.dealit.domain.wallet.dto;

public record WalletResponse(
	Long walletId,
	Long memberId,
	Long balance
) {
}
