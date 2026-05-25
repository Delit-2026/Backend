package com.dealit.dealit.domain.purchase.service;

import com.dealit.dealit.domain.auth.exception.InvalidCredentialsException;
import com.dealit.dealit.domain.auction.AuctionPaymentStatus;
import com.dealit.dealit.domain.auction.entity.Auction;
import com.dealit.dealit.domain.auction.entity.AuctionPayment;
import com.dealit.dealit.domain.auction.repository.AuctionPaymentRepository;
import com.dealit.dealit.domain.chat.entity.ChatRoom;
import com.dealit.dealit.domain.chat.entity.ChatType;
import com.dealit.dealit.domain.chat.repository.ChatRoomRepository;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.exception.EmailNotVerifiedException;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.notification.dto.NotificationCreateRequest;
import com.dealit.dealit.domain.notification.entity.InAppNotificationType;
import com.dealit.dealit.domain.notification.service.FcmNotificationService;
import com.dealit.dealit.domain.notification.service.NotificationCenterService;
import com.dealit.dealit.domain.payment.service.ProductPaymentService;
import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.entity.ProductImage;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import com.dealit.dealit.domain.purchase.dto.MyPurchaseItemResponse;
import com.dealit.dealit.domain.purchase.dto.MyPurchaseListResponse;
import com.dealit.dealit.domain.purchase.dto.MySaleItemResponse;
import com.dealit.dealit.domain.purchase.dto.MySaleListResponse;
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
import com.dealit.dealit.domain.review.repository.ReviewRepository;
import com.dealit.dealit.domain.wallet.exception.InvalidWalletRequestException;
import com.dealit.dealit.domain.wallet.service.WalletService;
import com.dealit.dealit.global.service.ImageUrlService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseService {

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

	private final PurchaseRepository purchaseRepository;
	private final AuctionPaymentRepository auctionPaymentRepository;
	private final ReviewRepository reviewRepository;
	private final ProductRepository productRepository;
	private final MemberRepository memberRepository;
	private final WalletService walletService;
	private final ProductPaymentService productPaymentService;
	private final ImageUrlService imageUrlService;
	private final ChatRoomRepository chatRoomRepository;
	private final NotificationCenterService notificationCenterService;
	private final FcmNotificationService fcmNotificationService;

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
			notifyPurchasePaid(purchase, product);
			return toPurchaseResponse(purchase);
		}

	@Transactional
	public Purchase createAuctionPurchase(Auction auction, AuctionPayment winningPayment, Long chatRoomId) {
		if (auction == null || winningPayment == null) {
			throw new IllegalArgumentException("낙찰 경매와 결제 정보는 필수입니다.");
		}

		Product product = auction.getProduct();
		String idempotencyKey = "auction-" + auction.getAuctionId();
		Purchase purchase = purchaseRepository.findByBuyerIdAndIdempotencyKey(
				winningPayment.getBidderId(),
				idempotencyKey
			)
			.orElseGet(() -> purchaseRepository.save(Purchase.paid(
				product.getProductId(),
				winningPayment.getBidderId(),
				winningPayment.getSellerId(),
				BigDecimal.valueOf(winningPayment.getAmount()),
				idempotencyKey
			)));

		if (chatRoomId != null) {
			purchase.linkChatRoom(chatRoomId);
		} else {
			linkPurchaseChatRoomIfNeeded(purchase);
		}
		winningPayment.linkPurchase(purchase.getPurchaseId());
		return purchase;
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
			purchase.getChatRoomId(),
			product.getSaleType(),
			toSeoulOffsetDateTime(purchase.getPurchasedAt()),
			toSeoulOffsetDateTime(purchase.getShippedAt()),
			toSeoulOffsetDateTime(purchase.getCompletedAt()),
			toSeoulOffsetDateTime(purchase.getCanceledAt()),
			purchase.getShippedAt() != null,
			purchase.getBuyerCompletedAt() != null,
			resolveCounterpartNickname(purchase, memberId)
		);
	}

	public MyPurchaseListResponse getMyPurchases(
		Long memberId,
		int page,
		int size,
		List<PurchaseStatus> statuses
	) {
		int normalizedPage = Math.max(page, 0);
		int normalizedSize = Math.min(Math.max(size, 1), 100);
		PageRequest pageRequest = PageRequest.of(normalizedPage, normalizedSize);

		Page<Purchase> purchasePage = statuses == null || statuses.isEmpty()
			? purchaseRepository.findByBuyerIdOrderByPurchaseIdDesc(memberId, pageRequest)
			: purchaseRepository.findByBuyerIdAndStatusInOrderByPurchaseIdDesc(memberId, statuses, pageRequest);

		List<Purchase> purchases = purchasePage.getContent();
		Map<Long, Product> productsById = loadProductsById(purchases);
		Map<Long, AuctionPayment> auctionPaymentsByPurchaseId = loadAuctionPaymentsByPurchaseId(purchases);
		Set<Long> reviewedRegularProductIds = loadReviewedRegularProductIds(
			memberId,
			purchases,
			auctionPaymentsByPurchaseId
		);
		Set<Long> reviewedAuctionIds = loadReviewedAuctionIds(memberId, auctionPaymentsByPurchaseId);
		List<MyPurchaseItemResponse> content = purchases.stream()
			.map(purchase -> toMyPurchaseItemResponse(
				purchase,
				productsById.get(purchase.getProductId()),
				auctionPaymentsByPurchaseId.get(purchase.getPurchaseId()),
				reviewedRegularProductIds,
				reviewedAuctionIds
			))
			.toList();

		return new MyPurchaseListResponse(
			content,
			purchasePage.getNumber(),
			purchasePage.getSize(),
			purchasePage.getTotalElements(),
			purchasePage.hasNext()
		);
	}

	public MySaleListResponse getMySales(
		Long memberId,
		int page,
		int size,
		List<PurchaseStatus> statuses
	) {
		int normalizedPage = Math.max(page, 0);
		int normalizedSize = Math.min(Math.max(size, 1), 100);
		PageRequest pageRequest = PageRequest.of(normalizedPage, normalizedSize);

		Page<Purchase> purchasePage = statuses == null || statuses.isEmpty()
			? purchaseRepository.findBySellerIdOrderByPurchaseIdDesc(memberId, pageRequest)
			: purchaseRepository.findBySellerIdAndStatusInOrderByPurchaseIdDesc(memberId, statuses, pageRequest);

		List<Purchase> purchases = purchasePage.getContent();
		Map<Long, Product> productsById = loadProductsById(purchases);
		Map<Long, AuctionPayment> auctionPaymentsByPurchaseId = loadAuctionPaymentsByPurchaseId(purchases);
		Set<ReviewProductKey> regularReviewKeys = loadRegularReviewKeys(purchases, auctionPaymentsByPurchaseId);
		Set<ReviewAuctionKey> auctionReviewKeys = loadAuctionReviewKeys(purchases, auctionPaymentsByPurchaseId);
		List<MySaleItemResponse> content = purchases.stream()
			.map(purchase -> toMySaleItemResponse(
				purchase,
				productsById.get(purchase.getProductId()),
				auctionPaymentsByPurchaseId.get(purchase.getPurchaseId()),
				regularReviewKeys,
				auctionReviewKeys
			))
			.toList();

		return new MySaleListResponse(
			content,
			purchasePage.getNumber(),
			purchasePage.getSize(),
			purchasePage.getTotalElements(),
			purchasePage.hasNext()
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
		markLinkedAuctionPaymentShipped(purchase);
		notifyPurchaseShipped(purchase);
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

	private Map<Long, Product> loadProductsById(List<Purchase> purchases) {
		List<Long> productIds = purchases.stream()
			.map(Purchase::getProductId)
			.distinct()
			.toList();

		if (productIds.isEmpty()) {
			return Map.of();
		}
		return productRepository.findAllByProductIdInAndDeletedAtIsNull(productIds).stream()
			.collect(Collectors.toMap(Product::getProductId, Function.identity()));
	}

	private Map<Long, AuctionPayment> loadAuctionPaymentsByPurchaseId(List<Purchase> purchases) {
		List<Long> purchaseIds = purchases.stream()
			.map(Purchase::getPurchaseId)
			.distinct()
			.toList();

		if (purchaseIds.isEmpty()) {
			return Map.of();
		}
		return auctionPaymentRepository.findAllByPurchaseIdInAndDeletedAtIsNull(purchaseIds).stream()
			.collect(Collectors.toMap(AuctionPayment::getPurchaseId, Function.identity()));
	}

	private Set<Long> loadReviewedRegularProductIds(
		Long reviewerId,
		List<Purchase> purchases,
		Map<Long, AuctionPayment> auctionPaymentsByPurchaseId
	) {
		Set<Long> productIds = purchases.stream()
			.filter(purchase -> !auctionPaymentsByPurchaseId.containsKey(purchase.getPurchaseId()))
			.map(Purchase::getProductId)
			.collect(Collectors.toSet());

		if (productIds.isEmpty()) {
			return Set.of();
		}
		return new HashSet<>(reviewRepository.findReviewedRegularProductIds(reviewerId, productIds));
	}

	private Set<Long> loadReviewedAuctionIds(
		Long reviewerId,
		Map<Long, AuctionPayment> auctionPaymentsByPurchaseId
	) {
		Set<Long> auctionIds = auctionPaymentsByPurchaseId.values().stream()
			.map(payment -> payment.getAuction().getAuctionId())
			.collect(Collectors.toSet());

		if (auctionIds.isEmpty()) {
			return Set.of();
		}
		return new HashSet<>(reviewRepository.findReviewedAuctionIds(reviewerId, auctionIds));
	}

	private Set<ReviewProductKey> loadRegularReviewKeys(
		List<Purchase> purchases,
		Map<Long, AuctionPayment> auctionPaymentsByPurchaseId
	) {
		Set<Long> buyerIds = purchases.stream()
			.map(Purchase::getBuyerId)
			.collect(Collectors.toSet());
		Set<Long> productIds = purchases.stream()
			.filter(purchase -> !auctionPaymentsByPurchaseId.containsKey(purchase.getPurchaseId()))
			.map(Purchase::getProductId)
			.collect(Collectors.toSet());

		if (buyerIds.isEmpty() || productIds.isEmpty()) {
			return Set.of();
		}
		return reviewRepository.findRegularReviewsByReviewersAndProducts(buyerIds, productIds).stream()
			.map(review -> new ReviewProductKey(review.getReviewerId(), review.getProductId()))
			.collect(Collectors.toSet());
	}

	private Set<ReviewAuctionKey> loadAuctionReviewKeys(
		List<Purchase> purchases,
		Map<Long, AuctionPayment> auctionPaymentsByPurchaseId
	) {
		Set<Long> buyerIds = purchases.stream()
			.map(Purchase::getBuyerId)
			.collect(Collectors.toSet());
		Set<Long> auctionIds = auctionPaymentsByPurchaseId.values().stream()
			.map(payment -> payment.getAuction().getAuctionId())
			.collect(Collectors.toSet());

		if (buyerIds.isEmpty() || auctionIds.isEmpty()) {
			return Set.of();
		}
		return reviewRepository.findAuctionReviewsByReviewersAndAuctions(buyerIds, auctionIds).stream()
			.map(review -> new ReviewAuctionKey(review.getReviewerId(), review.getAuctionId()))
			.collect(Collectors.toSet());
	}

	private MyPurchaseItemResponse toMyPurchaseItemResponse(
		Purchase purchase,
		Product product,
		AuctionPayment auctionPayment,
		Set<Long> reviewedRegularProductIds,
		Set<Long> reviewedAuctionIds
	) {
		Long auctionId = auctionPayment == null ? null : auctionPayment.getAuction().getAuctionId();
		ProductSaleType productType = product == null ? null : product.getSaleType();
		boolean reviewWritten = auctionId == null
			? reviewedRegularProductIds.contains(purchase.getProductId())
			: reviewedAuctionIds.contains(auctionId);
		boolean completed = purchase.getStatus() == PurchaseStatus.COMPLETED;
		return new MyPurchaseItemResponse(
			purchase.getPurchaseId(),
			purchase.getProductId(),
			product == null ? null : product.getName(),
			product == null ? null : resolveThumbnailUrl(product),
			purchase.getSellerId(),
			toWalletAmount(purchase.getPriceSnapshot()),
			purchase.getStatus(),
			toSeoulOffsetDateTime(purchase.getPurchasedAt()),
			purchase.getChatRoomId(),
			productType,
			auctionId,
			purchase.getShippedAt() != null,
			purchase.getBuyerCompletedAt() != null,
			completed,
			toSeoulOffsetDateTime(purchase.getShippedAt()),
			toSeoulOffsetDateTime(purchase.getCompletedAt()),
			reviewWritten,
			completed && !reviewWritten
		);
	}

	private MySaleItemResponse toMySaleItemResponse(
		Purchase purchase,
		Product product,
		AuctionPayment auctionPayment,
		Set<ReviewProductKey> regularReviewKeys,
		Set<ReviewAuctionKey> auctionReviewKeys
	) {
		Long auctionId = auctionPayment == null ? null : auctionPayment.getAuction().getAuctionId();
		ProductSaleType productType = product == null ? null : product.getSaleType();
		boolean reviewReceived = auctionId == null
			? regularReviewKeys.contains(new ReviewProductKey(purchase.getBuyerId(), purchase.getProductId()))
			: auctionReviewKeys.contains(new ReviewAuctionKey(purchase.getBuyerId(), auctionId));
		boolean completed = purchase.getStatus() == PurchaseStatus.COMPLETED;
		return new MySaleItemResponse(
			purchase.getPurchaseId(),
			purchase.getProductId(),
			product == null ? null : product.getName(),
			product == null ? null : resolveThumbnailUrl(product),
			purchase.getBuyerId(),
			toWalletAmount(purchase.getPriceSnapshot()),
			purchase.getStatus(),
			toSeoulOffsetDateTime(purchase.getPurchasedAt()),
			purchase.getChatRoomId(),
			productType,
			auctionId,
			purchase.getShippedAt() != null,
			purchase.getBuyerCompletedAt() != null,
			completed,
			toSeoulOffsetDateTime(purchase.getShippedAt()),
			toSeoulOffsetDateTime(purchase.getCompletedAt()),
			reviewReceived
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
		notifyPurchaseReceived(purchase);
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
		Optional<AuctionPayment> linkedAuctionPayment = findLinkedAuctionPayment(purchase);
		if (linkedAuctionPayment.isPresent()) {
			AuctionPayment payment = linkedAuctionPayment.get();
			if (payment.getStatus() == AuctionPaymentStatus.RESERVED) {
				payment.requestRefund(toSeoulOffsetDateTime(LocalDateTime.now()));
			}
			return 1;
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
		Optional<AuctionPayment> linkedAuctionPayment = findLinkedAuctionPayment(purchase);
		if (linkedAuctionPayment.isPresent()) {
			AuctionPayment payment = linkedAuctionPayment.get();
			if (payment.getStatus() == AuctionPaymentStatus.SETTLED) {
				purchase.settle();
				return;
			}
			if (payment.confirmReceived(toSeoulOffsetDateTime(LocalDateTime.now()))) {
				walletService.settleAuctionPayment(
					payment.getSellerId(),
					payment.getAmount(),
					payment.getAuction().getAuctionId()
				);
				purchase.settle();
			}
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

	@Transactional
	public void syncAuctionPurchaseShipped(Long purchaseId) {
		if (purchaseId == null) {
			return;
		}
		Purchase purchase = purchaseRepository.findById(purchaseId).orElse(null);
		if (purchase == null || purchase.getStatus() != PurchaseStatus.PAID) {
			return;
		}
		try {
			purchase.markShipped();
		} catch (IllegalStateException ignored) {
		}
	}

	@Transactional
	public void syncAuctionPurchaseCompleted(Long purchaseId) {
		if (purchaseId == null) {
			return;
		}
		Purchase purchase = purchaseRepository.findById(purchaseId).orElse(null);
		if (purchase == null || purchase.getStatus() == PurchaseStatus.CANCELED) {
			return;
		}
		try {
			if (purchase.getStatus() == PurchaseStatus.PAID) {
				purchase.markShipped();
			}
			purchase.markBuyerCompleted();
			purchase.complete();
			purchase.settle();
		} catch (IllegalStateException ignored) {
		}
	}

	@Transactional
	public void syncAuctionPurchaseCanceled(Long purchaseId) {
		if (purchaseId == null) {
			return;
		}
		Purchase purchase = purchaseRepository.findById(purchaseId).orElse(null);
		if (purchase == null || purchase.getStatus() != PurchaseStatus.PAID) {
			return;
		}
		try {
			purchase.cancel();
		} catch (IllegalStateException ignored) {
		}
	}

	private void markLinkedAuctionPaymentShipped(Purchase purchase) {
		findLinkedAuctionPayment(purchase)
			.filter(payment -> payment.getStatus() == AuctionPaymentStatus.RESERVED)
			.ifPresent(payment -> payment.markShipped(toSeoulOffsetDateTime(LocalDateTime.now())));
	}

	private Optional<AuctionPayment> findLinkedAuctionPayment(Purchase purchase) {
		return auctionPaymentRepository.findByPurchaseIdAndDeletedAtIsNull(purchase.getPurchaseId());
	}

	private void markProductEnded(Purchase purchase) {
		productRepository.findByProductIdAndDeletedAtIsNull(purchase.getProductId())
			.ifPresent(Product::markTradeCompleted);
	}

	private void notifyPurchasePaid(Purchase purchase, Product product) {
		String sellerTitle = "상품이 판매되었습니다.";
		String sellerContent = "'" + product.getName() + "' 상품 결제가 완료되었어요. 물건을 보내주세요.";
		sendTradeNotification(
			purchase.getSellerId(),
			sellerTitle,
			sellerContent,
			"PURCHASE_PAID",
			purchase
		);

		String buyerTitle = "구매가 완료되었습니다.";
		String buyerContent = "'" + product.getName() + "' 상품 구매가 완료되었어요. 판매자의 발송을 기다려주세요.";
		sendTradeNotification(
			purchase.getBuyerId(),
			buyerTitle,
			buyerContent,
			"PURCHASE_COMPLETED",
			purchase
		);
	}

	private void notifyPurchaseShipped(Purchase purchase) {
		String productName = resolveProductName(purchase);
		String title = "상품이 발송되었습니다.";
		String content = "'" + productName + "' 상품이 발송되었어요. 물건을 받으면 수령확정을 눌러주세요.";
		sendTradeNotification(
			purchase.getBuyerId(),
			title,
			content,
			"PURCHASE_SHIPPED",
			purchase
		);
	}

	private void notifyPurchaseReceived(Purchase purchase) {
		String productName = resolveProductName(purchase);
		String title = "수령확정이 완료되었습니다.";
		String content = "'" + productName + "' 상품 구매자가 수령확정을 완료했어요.";
		sendTradeNotification(
			purchase.getSellerId(),
			title,
			content,
			"PURCHASE_RECEIVED",
			purchase
		);
	}

	private void sendTradeNotification(
		Long receiverId,
		String title,
		String content,
		String type,
		Purchase purchase
	) {
		String targetUrl = "/products/" + purchase.getProductId()
			+ "/receipt?purchaseId=" + purchase.getPurchaseId();

		notificationCenterService.create(
			receiverId,
			new NotificationCreateRequest(
				InAppNotificationType.TRADE,
				title,
				content,
				"RECEIPT",
				purchase.getPurchaseId(),
				targetUrl
			)
		);

		try {
			int sentCount = fcmNotificationService.sendToMember(
				receiverId,
				title,
				content,
				Map.of(
					"type", type,
					"purchaseId", String.valueOf(purchase.getPurchaseId()),
					"productId", String.valueOf(purchase.getProductId()),
					"targetUrl", targetUrl
				)
			);
			log.debug("Sent purchase push notification. type={}, purchaseId={}, receiverId={}, sentCount={}",
				type, purchase.getPurchaseId(), receiverId, sentCount);
		} catch (RuntimeException exception) {
			log.warn("Failed to send purchase push notification. type={}, purchaseId={}, receiverId={}",
				type, purchase.getPurchaseId(), receiverId, exception);
		}
	}

	private String resolveProductName(Purchase purchase) {
		return productRepository.findByProductIdAndDeletedAtIsNull(purchase.getProductId())
			.map(Product::getName)
			.orElse("구매한");
	}

	private String resolveCounterpartNickname(Purchase purchase, Long memberId) {
		Long counterpartId = purchase.getBuyerId().equals(memberId)
			? purchase.getSellerId()
			: purchase.getBuyerId();

		return memberRepository.findByMemberIdAndDeletedAtIsNull(counterpartId)
			.map(Member::getNickname)
			.orElse(null);
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

	private record ReviewProductKey(Long reviewerId, Long productId) {
	}

	private record ReviewAuctionKey(Long reviewerId, Long auctionId) {
	}
}
