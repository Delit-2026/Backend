package com.dealit.dealit.domain.wallet.dto;

import java.util.List;

public record WalletLedgerListResponse(
	List<WalletLedgerResponse> content,
	int page,
	int size,
	long totalElements,
	boolean hasNext
) {
}
