package com.dealit.dealit.domain.purchase.service;

import com.dealit.dealit.domain.auth.exception.InvalidCredentialsException;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.exception.EmailNotVerifiedException;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.entity.ProductImage;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import com.dealit.dealit.domain.purchase.dto.PurchaseReceiptResponse;
import com.dealit.dealit.domain.purchase.dto.PurchaseResponse;
import com.dealit.dealit.domain.purchase.entity.Purchase;
import com.dealit.dealit.domain.purchase.exception.IdempotencyConflictException;
import com.dealit.dealit.domain.purchase.exception.InsufficientBalanceException;
import com.dealit.dealit.domain.purchase.exception.ProductNotPurchasableException;
import com.dealit.dealit.domain.purchase.exception.PurchaseForbiddenException;
import com.dealit.dealit.domain.purchase.exception.PurchaseNotFoundException;
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
	private final ImageUrlService imageUrlService;

	@Transactional
	public PurchaseResponse purchase(Long productId, Long buyerId, String idempotencyKey) {
		validateIdempotencyKey(idempotencyKey);

		Purchase existingPurchase = purchaseRepository.findByBuyerIdAndIdempotencyKey(buyerId, idempotencyKey)
			.orElse(null);
		if (existingPurchase != null) {
			if (!existingPurchase.getProductId().equals(productId)) {
				throw new IdempotencyConflictException();
			}
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

		product.markSold();
		return toPurchaseResponse(purchase);
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
