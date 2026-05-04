package com.dealit.dealit.domain.wallet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WalletAmountRequest(
	@NotNull(message = "금액은 필수입니다.")
	@Min(value = 1, message = "금액은 1원 이상이어야 합니다.")
	@Max(value = 10000000, message = "금액은 10,000,000원 이하이어야 합니다.")
	Long amount
) {
}
