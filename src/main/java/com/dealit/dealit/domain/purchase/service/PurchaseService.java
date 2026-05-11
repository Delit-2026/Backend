package com.dealit.dealit.domain.purchase.service;

import com.dealit.dealit.domain.auth.exception.InvalidCredentialsException;
import com.dealit.dealit.domain.chat.entity.ChatRoom;
import com.dealit.dealit.domain.chat.entity.ChatType;
import com.dealit.dealit.domain.chat.repository.ChatRoomRepository;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.exception.EmailNotVerifiedException;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.payment.service.ProductPaymentService;
import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.entity.ProductImage;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import com.dealit.dealit.domain.purchase.dto.PurchaseCompletionResponse;
import com.dealit.dealit.domain.purchase.dto.PurchaseReceiptResponse;
import com.dealit.dealit.domain.purchase.dto.PurchaseResponse;
import com.dealit.dealit.domain.purchase.entity.Purchase;
import com.dealit.dealit.domain.purchase.entity.PurchaseStatus;
import com.dealit.dealit.domain.purchase.exception.IdempotencyConflictException;
import com.dealit.dealit.domain.purchase.exception.InsufficientBalanceException;
import com.dealit.dealit.domain.purchase.exception.ProductNotPurchasableException;
import com.dealit.dealit.domain.purchase.exception.PurchaseForbiddenException;
import com.dealit.dealit.domain.purchase.exception.PurchaseNotFoundException;
import com.dealit.dealit.domain.purchase.exception.PurchaseNotCompletableException;
import com.dealit.dealit.domain.purchase.repository.PurchaseRepository;
import com.dealit.dealit.domain.wallet.exception.InvalidWalletRequestException;
import com.dealit.dealit.domain.wallet.service.WalletService;
import com.dealit.dealit.global.service.ImageUrlService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseService {

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

	private final PurchaseRepository purchaseRepository;
	private final ProductRepository productRepository;
	private final MemberRepository memberRepository;
	private final WalletService walletService;
	private final ProductPaymentService productPaymentService;
	private final ImageUrlService imageUrlService;
	private final ChatRoomRepository chatRoomRepository;

	@Transactional
	public PurchaseResponse purchase(Long productId, Long buyerId, String idempotencyKey) {
		validateIdempotencyKey(idempotencyKey);

		Purchase existingPurchase = purchaseRepository.findByBuyerIdAndIdempotencyKey(buyerId, idempotencyKey)
			.orElse(null);
			if (existingPurchase != null) {
				if (!existingPurchase.getProductId().equals(productId)) {
					throw new IdempotencyConflictException();
				}
				linkPurchaseChatRoomIfNeeded(existingPurchase);
				return toPurchaseResponse(existingPurchase);
			}

		Member buyer = memberRepository.findByMemberIdAndDeletedAtIsNull(buyerId)
			.orElseThrow(() -> new InvalidCredentialsException("존재하지 않는 회원입니다."));
		if (!buyer.isVerified()) {
			throw new EmailNotVerifiedException();
		}

		Product product = productRepository.findWithLockByProductIdAndDeletedAtIsNull(productId)
			.orElseThrow(ProductNotPurchasableException::new);
		validatePurchasable(product, buyerId);

		long amount = toWalletAmount(product.getPrice());
		Purchase purchase = purchaseRepository.save(Purchase.paid(
			product.getProductId(),
			buyerId,
			product.getMemberId(),
			product.getPrice(),
			idempotencyKey
		));

		try {
			walletService.payForPurchase(buyerId, amount, purchase.getPurchaseId());
		} catch (InvalidWalletRequestException exception) {
			throw new InsufficientBalanceException();
		}
		productPaymentService.hold(
			purchase.getPurchaseId(),
			purchase.getProductId(),
			purchase.getBuyerId(),
			purchase.getSellerId(),
			amount
			);

			product.markSold();
			linkPurchaseChatRoomIfNeeded(purchase);
			return toPurchaseResponse(purchase);
		}

	private void linkPurchaseChatRoomIfNeeded(Purchase purchase) {
		if (purchase.getChatRoomId() != null) {
			return;
		}

		ChatRoom chatRoom = chatRoomRepository
			.findBySellerIdAndBuyerIdAndProductIdAndDeletedAtIsNull(
				purchase.getSellerId(),
				purchase.getBuyerId(),
				purchase.getProductId()
			)
			.orElseGet(() -> chatRoomRepository.save(
				ChatRoom.create(
					purchase.getSellerId(),
					purchase.getBuyerId(),
					purchase.getProductId(),
					ChatType.GENERAL
				)
			));
		purchase.linkChatRoom(chatRoom.getRoomId());
	}

	public PurchaseReceiptResponse getReceipt(Long purchaseId, Long memberId) {
		Purchase purchase = purchaseRepository.findById(purchaseId)
			.orElseThrow(PurchaseNotFoundException::new);
		if (!purchase.getBuyerId().equals(memberId) && !purchase.getSellerId().equals(memberId)) {
			throw new PurchaseForbiddenException();
		}

		Product product = productRepository.findByProductIdAndDeletedAtIsNull(purchase.getProductId())
			.orElseThrow(PurchaseNotFoundException::new);

		return new PurchaseReceiptResponse(
			purchase.getPurchaseId(),
			purchase.getProductId(),
			product.getName(),
			resolveThumbnailUrl(product),
			purchase.getBuyerId(),
			purchase.getSellerId(),
			toWalletAmount(purchase.getPriceSnapshot()),
			purchase.getStatus(),
			toSeoulOffsetDateTime(purchase.getPurchasedAt()),
			purchase.getChatRoomId()
		);
	}

	@Transactional
	public PurchaseCompletionResponse completeByBuyer(Long purchaseId, Long buyerId) {
		Purchase purchase = purchaseRepository.findById(purchaseId)
			.orElseThrow(PurchaseNotFoundException::new);
		if (!purchase.getBuyerId().equals(buyerId)) {
			throw new PurchaseForbiddenException();
		}
		receiveAndSettle(purchase, "구매자 수령확정");
		return toCompletionResponse(purchase);
	}

	@Transactional
	public PurchaseCompletionResponse completeBySeller(Long purchaseId, Long sellerId) {
		return shipBySeller(purchaseId, sellerId);
	}

	@Transactional
	public PurchaseCompletionResponse shipBySeller(Long purchaseId, Long sellerId) {
		Purchase purchase = purchaseRepository.findById(purchaseId)
			.orElseThrow(PurchaseNotFoundException::new);
		if (!purchase.getSellerId().equals(sellerId)) {
			throw new PurchaseForbiddenException();
		}
		try {
			purchase.markShipped();
		} catch (IllegalStateException exception) {
			throw new PurchaseNotCompletableException(exception.getMessage());
		}
		return toCompletionResponse(purchase);
	}

	@Transactional
	public PurchaseCompletionResponse receiveByBuyer(Long purchaseId, Long buyerId) {
		return completeByBuyer(purchaseId, buyerId);
	}

	@Transactional
	public int cancelExpiredUnshippedPurchases(LocalDateTime now) {
		return purchaseRepository
			.findTop100ByStatusAndShippingDeadlineAtLessThanEqualOrderByShippingDeadlineAtAsc(PurchaseStatus.PAID, now)
			.stream()
			.mapToInt(this::cancelAndRefund)
			.sum();
	}

	@Transactional
	public int autoCompleteShippedPurchases(LocalDateTime now) {
		return purchaseRepository
			.findTop100ByStatusAndAutoCompleteAtLessThanEqualOrderByAutoCompleteAtAsc(PurchaseStatus.SHIPPED, now)
			.stream()
			.mapToInt(purchase -> autoCompleteAndSettle(purchase, "발송 후 7일 자동 수령확정"))
			.sum();
	}

	private void validateIdempotencyKey(String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new IllegalArgumentException("idempotencyKey는 필수입니다.");
		}

		try {
			UUID.fromString(idempotencyKey);
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("idempotencyKey는 UUID 형식이어야 합니다.");
		}
	}

	private void validatePurchasable(Product product, Long buyerId) {
		if (product.getSaleType() != ProductSaleType.REGULAR) {
			throw new ProductNotPurchasableException();
		}
		if (product.getStatus() != ProductStatus.ON_SALE) {
			throw new ProductNotPurchasableException();
		}
		if (product.getMemberId().equals(buyerId)) {
			throw new ProductNotPurchasableException("본인 상품은 구매할 수 없습니다.");
		}
	}

	private long toWalletAmount(BigDecimal price) {
		if (price == null || price.signum() <= 0 || price.stripTrailingZeros().scale() > 0) {
			throw new ProductNotPurchasableException();
		}

		try {
			return price.longValueExact();
		} catch (ArithmeticException exception) {
			throw new ProductNotPurchasableException();
		}
	}

	private PurchaseResponse toPurchaseResponse(Purchase purchase) {
		return new PurchaseResponse(
			purchase.getPurchaseId(),
			purchase.getProductId(),
			toWalletAmount(purchase.getPriceSnapshot()),
			purchase.getStatus(),
			toSeoulOffsetDateTime(purchase.getPurchasedAt())
		);
	}

	private void receiveAndSettle(Purchase purchase, String settlementReason) {
		try {
			purchase.markBuyerCompleted();
		} catch (IllegalStateException exception) {
			throw new PurchaseNotCompletableException(exception.getMessage());
		}
		purchase.complete();
		markProductEnded(purchase);
		settleIfNeeded(purchase, settlementReason);
	}

	private int autoCompleteAndSettle(Purchase purchase, String settlementReason) {
		if (purchase.getStatus() != PurchaseStatus.SHIPPED) {
			return 0;
		}
		receiveAndSettle(purchase, settlementReason);
		return 1;
	}

	private int cancelAndRefund(Purchase purchase) {
		if (purchase.getStatus() != PurchaseStatus.PAID) {
			return 0;
		}
		try {
			purchase.cancel();
		} catch (IllegalStateException exception) {
			return 0;
		}
		productPaymentService.findByPurchaseId(purchase.getPurchaseId())
			.ifPresentOrElse(
				payment -> productPaymentService.refund(payment, "판매자 3일 내 미발송 자동 취소"),
				() -> walletService.refund(purchase.getBuyerId(), toWalletAmount(purchase.getPriceSnapshot()))
			);
		return 1;
	}

	private void settleIfNeeded(Purchase purchase, String settlementReason) {
		if (purchase.isSettled()) {
			return;
		}
		productPaymentService.findByPurchaseId(purchase.getPurchaseId())
			.ifPresentOrElse(
				payment -> productPaymentService.settle(payment, settlementReason),
				() -> walletService.settlePurchase(
					purchase.getSellerId(),
					toWalletAmount(purchase.getPriceSnapshot()),
					purchase.getPurchaseId()
				)
			);
		purchase.settle();
	}

	private void markProductEnded(Purchase purchase) {
		productRepository.findByProductIdAndDeletedAtIsNull(purchase.getProductId())
			.ifPresent(Product::markTradeCompleted);
	}

	private PurchaseCompletionResponse toCompletionResponse(Purchase purchase) {
		return new PurchaseCompletionResponse(
			purchase.getPurchaseId(),
			purchase.getStatus(),
			purchase.getBuyerCompletedAt() != null,
			purchase.getSellerCompletedAt() != null,
			toSeoulOffsetDateTime(purchase.getBuyerCompletedAt()),
			toSeoulOffsetDateTime(purchase.getSellerCompletedAt()),
			toSeoulOffsetDateTime(purchase.getShippedAt()),
			toSeoulOffsetDateTime(purchase.getAutoCompleteAt()),
			toSeoulOffsetDateTime(purchase.getCompletedAt()),
			toSeoulOffsetDateTime(purchase.getCanceledAt()),
			toSeoulOffsetDateTime(purchase.getSettledAt())
		);
	}

	private String resolveThumbnailUrl(Product product) {
		return product.getImages().stream()
			.filter(image -> image.getDeletedAt() == null)
			.filter(image -> image.getImageUrl() != null && !image.getImageUrl().isBlank())
			.sorted(Comparator.comparing(
				ProductImage::getSortOrder,
				Comparator.nullsLast(Integer::compareTo)
			))
			.map(image -> imageUrlService.toPublicUrl(image.getImageUrl()))
			.findFirst()
			.orElse(null);
	}

	private OffsetDateTime toSeoulOffsetDateTime(LocalDateTime dateTime) {
		if (dateTime == null) {
			return null;
		}
		return dateTime.atZone(SEOUL_ZONE).toOffsetDateTime();
	}
}
