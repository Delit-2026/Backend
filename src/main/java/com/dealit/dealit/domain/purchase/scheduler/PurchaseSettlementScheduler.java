package com.dealit.dealit.domain.purchase.scheduler;

import com.dealit.dealit.domain.purchase.service.PurchaseService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseSettlementScheduler {

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

	private final PurchaseService purchaseService;
	private final Clock clock;

	@Scheduled(fixedDelayString = "${app.purchase.settlement-scheduler-delay-ms:60000}")
	public void processExpiredPurchases() {
		LocalDateTime now = LocalDateTime.now(clock.withZone(SEOUL_ZONE));
		purchaseService.cancelExpiredUnshippedPurchases(now);
		purchaseService.autoCompleteShippedPurchases(now);
	}
}
