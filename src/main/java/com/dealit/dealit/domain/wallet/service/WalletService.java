package com.dealit.dealit.domain.wallet.service;

import com.dealit.dealit.domain.auth.exception.InvalidCredentialsException;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.wallet.dto.WalletLedgerListResponse;
import com.dealit.dealit.domain.wallet.dto.WalletLedgerResponse;
import com.dealit.dealit.domain.wallet.dto.WalletResponse;
import com.dealit.dealit.domain.wallet.entity.Wallet;
import com.dealit.dealit.domain.wallet.entity.WalletLedger;
import com.dealit.dealit.domain.wallet.entity.WalletLedgerType;
import com.dealit.dealit.domain.wallet.exception.InvalidWalletRequestException;
import com.dealit.dealit.domain.wallet.repository.WalletLedgerRepository;
import com.dealit.dealit.domain.wallet.repository.WalletRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

	private final WalletRepository walletRepository;
	private final WalletLedgerRepository walletLedgerRepository;
	private final MemberRepository memberRepository;

	@Transactional
	public WalletResponse getMyWallet(Long memberId) {
		validateActiveMember(memberId);
		return toWalletResponse(findOrCreateWallet(memberId));
	}

	@Transactional
	public WalletResponse charge(Long memberId, long amount) {
		return apply(memberId, amount, WalletLedgerType.TEMP_CHARGE, "딜릿머니 임시 충전");
	}

	@Transactional
	public WalletResponse refund(Long memberId, long amount) {
		return apply(memberId, amount, WalletLedgerType.REFUND, "딜릿머니 환불");
	}

	@Transactional
	public WalletResponse withdraw(Long memberId, long amount) {
		return apply(memberId, -amount, WalletLedgerType.WITHDRAWAL, "내 계좌로 옮기기");
	}

	@Transactional
	public WalletResponse payForPurchase(Long buyerId, long amount, Long purchaseId) {
		if (amount <= 0) {
			throw new InvalidWalletRequestException("구매 금액은 0보다 커야 합니다.");
		}
		return apply(buyerId, -amount, WalletLedgerType.PURCHASE, "상품 구매 결제");
	}

	@Transactional
	public WalletResponse settlePurchase(Long sellerId, long amount, Long purchaseId) {
		if (amount <= 0) {
			throw new InvalidWalletRequestException("정산 금액은 0보다 커야 합니다.");
		}
		return apply(sellerId, amount, WalletLedgerType.SETTLEMENT, "상품 판매 정산");
	}

	@Transactional
	public long reserveAuctionBid(Long memberId, long amount, Long auctionId) {
		return applyForAuction(memberId, -amount, WalletLedgerType.AUCTION_RESERVE, "경매 입찰 예치금 차감: auctionId=%d".formatted(auctionId));
	}

	@Transactional
	public long refundAuctionPayment(Long memberId, long amount, Long auctionId) {
		return applyForAuction(memberId, amount, WalletLedgerType.AUCTION_REFUND, "경매 예치금 환불: auctionId=%d".formatted(auctionId));
	}

	@Transactional
	public long settleAuctionPayment(Long memberId, long amount, Long auctionId) {
		return applyForAuction(memberId, amount, WalletLedgerType.AUCTION_SETTLEMENT, "경매 판매대금 정산: auctionId=%d".formatted(auctionId));
	}

	public WalletLedgerListResponse getMyLedgers(Long memberId, int page, int size) {
		validateActiveMember(memberId);
		int normalizedPage = Math.max(page, 0);
		int normalizedSize = Math.min(Math.max(size, 1), 100);

		Page<WalletLedger> ledgerPage = walletLedgerRepository.findAllByMemberId(
			memberId,
			PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"))
		);

		return new WalletLedgerListResponse(
			ledgerPage.getContent().stream()
				.map(this::toLedgerResponse)
				.toList(),
			ledgerPage.getNumber(),
			ledgerPage.getSize(),
			ledgerPage.getTotalElements(),
			ledgerPage.hasNext()
		);
	}

	private WalletResponse apply(Long memberId, long signedAmount, WalletLedgerType type, String description) {
		applyForAuction(memberId, signedAmount, type, description);
		Wallet wallet = walletRepository.findByMemberId(memberId)
			.orElseThrow(() -> new InvalidWalletRequestException("지갑을 찾을 수 없습니다."));
		return toWalletResponse(wallet);
	}

	private long applyForAuction(Long memberId, long signedAmount, WalletLedgerType type, String description) {
		validateActiveMember(memberId);
		if (signedAmount == 0) {
			throw new InvalidWalletRequestException("금액은 0원일 수 없습니다.");
		}

		Wallet wallet = findOrCreateWallet(memberId);
		wallet = walletRepository.findWithLockByMemberId(memberId).orElse(wallet);

		long nextBalance;
		try {
			nextBalance = wallet.apply(signedAmount);
		} catch (IllegalArgumentException exception) {
			throw new InvalidWalletRequestException(exception.getMessage());
		}

		walletLedgerRepository.save(
			WalletLedger.create(wallet, type, signedAmount, nextBalance, description)
		);
		return nextBalance;
	}

	private Wallet findOrCreateWallet(Long memberId) {
		return walletRepository.findByMemberId(memberId)
			.orElseGet(() -> walletRepository.save(Wallet.create(memberId)));
	}

	private void validateActiveMember(Long memberId) {
		memberRepository.findByMemberIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new InvalidCredentialsException("존재하지 않는 회원입니다."));
	}

	private WalletResponse toWalletResponse(Wallet wallet) {
		return new WalletResponse(wallet.getWalletId(), wallet.getMemberId(), wallet.getBalance());
	}

	private WalletLedgerResponse toLedgerResponse(WalletLedger ledger) {
		return new WalletLedgerResponse(
			ledger.getWalletLedgerId(),
			ledger.getType(),
			ledger.getAmount(),
			ledger.getBalanceAfter(),
			ledger.getDescription(),
			toSeoulOffsetDateTime(ledger.getCreatedAt())
		);
	}

	private OffsetDateTime toSeoulOffsetDateTime(LocalDateTime dateTime) {
		if (dateTime == null) {
			return null;
		}
		return dateTime.atZone(SEOUL_ZONE).toOffsetDateTime();
	}
}
