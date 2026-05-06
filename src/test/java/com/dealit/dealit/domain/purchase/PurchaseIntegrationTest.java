package com.dealit.dealit.domain.purchase;

import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import com.dealit.dealit.domain.purchase.entity.Purchase;
import com.dealit.dealit.domain.purchase.entity.PurchaseStatus;
import com.dealit.dealit.domain.purchase.repository.PurchaseRepository;
import com.dealit.dealit.domain.wallet.entity.WalletLedgerType;
import com.dealit.dealit.domain.wallet.repository.WalletLedgerRepository;
import com.dealit.dealit.domain.wallet.repository.WalletRepository;
import com.dealit.dealit.domain.wallet.service.WalletService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PurchaseIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private PurchaseRepository purchaseRepository;

	@Autowired
	private WalletRepository walletRepository;

	@Autowired
	private WalletLedgerRepository walletLedgerRepository;

	@Autowired
	private WalletService walletService;

	private Member seller;
	private Member buyer;
	private Member otherBuyer;

	@BeforeEach
	void setUp() {
		purchaseRepository.deleteAll();
		walletLedgerRepository.deleteAll();
		walletRepository.deleteAll();
		productRepository.deleteAll();
		memberRepository.deleteAll();

		seller = saveMember("purchase-seller", "seller@example.com", "Purchase Seller", true);
		buyer = saveMember("purchase-buyer", "buyer@example.com", "Purchase Buyer", true);
		otherBuyer = saveMember("purchase-other-buyer", "other-buyer@example.com", "Purchase Other Buyer", true);
	}

	@Test
	@DisplayName("딜릿머니로 일반 상품을 구매하면 구매 기록, 지갑 차감, 원장, SOLD 처리가 수행된다")
	void purchaseRegularProductSuccess() throws Exception {
		Product product = saveProduct(seller, ProductStatus.ON_SALE, BigDecimal.valueOf(30000));
		walletService.charge(buyer.getMemberId(), 50000);

		mockMvc.perform(post("/api/v1/products/{productId}/purchase", product.getProductId())
				.with(authentication(authenticatedMember(buyer)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson(UUID.randomUUID().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.purchaseId").isNumber())
			.andExpect(jsonPath("$.productId").value(product.getProductId()))
			.andExpect(jsonPath("$.amount").value(30000))
			.andExpect(jsonPath("$.status").value("PAID"))
			.andExpect(jsonPath("$.purchasedAt").exists());

		assertThat(walletRepository.findByMemberId(buyer.getMemberId()).orElseThrow().getBalance()).isEqualTo(20000);
		assertThat(walletLedgerRepository.findAllByMemberId(
			buyer.getMemberId(),
			org.springframework.data.domain.Pageable.unpaged()
		).getContent())
			.anySatisfy(ledger -> {
				assertThat(ledger.getType()).isEqualTo(WalletLedgerType.PURCHASE);
				assertThat(ledger.getAmount()).isEqualTo(-30000);
				assertThat(ledger.getBalanceAfter()).isEqualTo(20000);
			});
		assertThat(productRepository.findById(product.getProductId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.SOLD);
		assertThat(purchaseRepository.findAll())
			.singleElement()
			.satisfies(purchase -> {
				assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.PAID);
				assertThat(purchase.getPriceSnapshot()).isEqualByComparingTo(BigDecimal.valueOf(30000));
			});
	}

	@Test
	@DisplayName("잔액이 부족하면 구매는 실패하고 상품과 지갑 상태는 유지된다")
	void purchaseFailsWhenBalanceInsufficient() throws Exception {
		Product product = saveProduct(seller, ProductStatus.ON_SALE, BigDecimal.valueOf(30000));
		walletService.charge(buyer.getMemberId(), 10000);

		mockMvc.perform(post("/api/v1/products/{productId}/purchase", product.getProductId())
				.with(authentication(authenticatedMember(buyer)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson(UUID.randomUUID().toString())))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));

		assertThat(productRepository.findById(product.getProductId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.ON_SALE);
		assertThat(walletRepository.findByMemberId(buyer.getMemberId()).orElseThrow().getBalance()).isEqualTo(10000);
		assertThat(countPurchaseLedgers(buyer.getMemberId())).isZero();
		assertThat(purchaseRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("이메일 미인증 사용자는 상품을 구매할 수 없다")
	void unverifiedBuyerPurchaseFails() throws Exception {
		Member unverifiedBuyer = saveMember(
			"purchase-unverified-buyer",
			"unverified-buyer@example.com",
			"Purchase Unverified Buyer",
			false
		);
		Product product = saveProduct(seller, ProductStatus.ON_SALE, BigDecimal.valueOf(30000));
		walletService.charge(unverifiedBuyer.getMemberId(), 50000);

		mockMvc.perform(post("/api/v1/products/{productId}/purchase", product.getProductId())
				.with(authentication(authenticatedMember(unverifiedBuyer)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson(UUID.randomUUID().toString())))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

		assertThat(productRepository.findById(product.getProductId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.ON_SALE);
		assertThat(walletRepository.findByMemberId(unverifiedBuyer.getMemberId()).orElseThrow().getBalance()).isEqualTo(50000);
		assertThat(countPurchaseLedgers(unverifiedBuyer.getMemberId())).isZero();
		assertThat(purchaseRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("UUID 형식이 아닌 idempotencyKey는 구매 요청을 실패시킨다")
	void invalidIdempotencyKeyPurchaseFails() throws Exception {
		Product product = saveProduct(seller, ProductStatus.ON_SALE, BigDecimal.valueOf(30000));
		walletService.charge(buyer.getMemberId(), 50000);

		mockMvc.perform(post("/api/v1/products/{productId}/purchase", product.getProductId())
				.with(authentication(authenticatedMember(buyer)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson("invalid-key")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		assertThat(productRepository.findById(product.getProductId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.ON_SALE);
		assertThat(walletRepository.findByMemberId(buyer.getMemberId()).orElseThrow().getBalance()).isEqualTo(50000);
		assertThat(countPurchaseLedgers(buyer.getMemberId())).isZero();
		assertThat(purchaseRepository.findAll()).isEmpty();
	}

	@Test
	@DisplayName("본인 상품은 구매할 수 없다")
	void purchaseOwnProductFails() throws Exception {
		Product product = saveProduct(buyer, ProductStatus.ON_SALE, BigDecimal.valueOf(30000));
		walletService.charge(buyer.getMemberId(), 50000);

		mockMvc.perform(post("/api/v1/products/{productId}/purchase", product.getProductId())
				.with(authentication(authenticatedMember(buyer)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson(UUID.randomUUID().toString())))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PRODUCT_NOT_PURCHASABLE"));
	}

	@Test
	@DisplayName("이미 SOLD인 상품은 구매할 수 없다")
	void soldProductPurchaseFails() throws Exception {
		Product product = saveProduct(seller, ProductStatus.SOLD, BigDecimal.valueOf(30000));
		walletService.charge(buyer.getMemberId(), 50000);

		mockMvc.perform(post("/api/v1/products/{productId}/purchase", product.getProductId())
				.with(authentication(authenticatedMember(buyer)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson(UUID.randomUUID().toString())))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PRODUCT_NOT_PURCHASABLE"));
	}

	@Test
	@DisplayName("같은 idempotencyKey로 같은 상품을 재요청하면 기존 구매 응답을 반환하고 중복 차감하지 않는다")
	void sameIdempotencyKeyReturnsExistingPurchase() throws Exception {
		Product product = saveProduct(seller, ProductStatus.ON_SALE, BigDecimal.valueOf(30000));
		walletService.charge(buyer.getMemberId(), 70000);
		String idempotencyKey = UUID.randomUUID().toString();

		String firstResponse = mockMvc.perform(post("/api/v1/products/{productId}/purchase", product.getProductId())
				.with(authentication(authenticatedMember(buyer)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson(idempotencyKey)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		mockMvc.perform(post("/api/v1/products/{productId}/purchase", product.getProductId())
				.with(authentication(authenticatedMember(buyer)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson(idempotencyKey)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.purchaseId").value(purchaseRepository.findAll().getFirst().getPurchaseId()));

		assertThat(firstResponse).contains("\"status\":\"PAID\"");
		assertThat(walletRepository.findByMemberId(buyer.getMemberId()).orElseThrow().getBalance()).isEqualTo(40000);
		assertThat(countPurchaseLedgers(buyer.getMemberId())).isEqualTo(1);
		assertThat(purchaseRepository.findAll()).hasSize(1);
	}

	@Test
	@DisplayName("같은 idempotencyKey로 다른 상품을 구매하려고 하면 충돌을 반환한다")
	void sameIdempotencyKeyForDifferentProductFails() throws Exception {
		Product firstProduct = saveProduct(seller, ProductStatus.ON_SALE, BigDecimal.valueOf(30000));
		Product secondProduct = saveProduct(seller, ProductStatus.ON_SALE, BigDecimal.valueOf(20000));
		walletService.charge(buyer.getMemberId(), 70000);
		String idempotencyKey = UUID.randomUUID().toString();

		mockMvc.perform(post("/api/v1/products/{productId}/purchase", firstProduct.getProductId())
				.with(authentication(authenticatedMember(buyer)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson(idempotencyKey)))
			.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/products/{productId}/purchase", secondProduct.getProductId())
				.with(authentication(authenticatedMember(buyer)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson(idempotencyKey)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));

		assertThat(purchaseRepository.findAll()).hasSize(1);
		assertThat(countPurchaseLedgers(buyer.getMemberId())).isEqualTo(1);
	}

	@Test
	@DisplayName("동시에 같은 상품을 구매하면 한 요청만 성공한다")
	void concurrentPurchaseAllowsOnlyOneSuccess() throws Exception {
		Product product = saveProduct(seller, ProductStatus.ON_SALE, BigDecimal.valueOf(30000));
		walletService.charge(buyer.getMemberId(), 50000);
		walletService.charge(otherBuyer.getMemberId(), 50000);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);

		Future<Integer> first = executor.submit(() -> performConcurrentPurchase(product.getProductId(), buyer, ready, start));
		Future<Integer> second = executor.submit(() -> performConcurrentPurchase(product.getProductId(), otherBuyer, ready, start));

		assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
		start.countDown();

		List<Integer> statuses = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
		executor.shutdown();

		assertThat(statuses).contains(200);
		assertThat(statuses).contains(409);
		assertThat(productRepository.findById(product.getProductId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.SOLD);
		assertThat(purchaseRepository.findAll()).hasSize(1);
		assertThat(walletLedgerRepository.findAll().stream()
			.filter(ledger -> ledger.getType() == WalletLedgerType.PURCHASE)
			.count()).isEqualTo(1);

		Purchase purchase = purchaseRepository.findAll().getFirst();
		Long successBuyerId = purchase.getBuyerId();
		Long failedBuyerId = successBuyerId.equals(buyer.getMemberId()) ? otherBuyer.getMemberId() : buyer.getMemberId();
		assertThat(walletRepository.findByMemberId(successBuyerId).orElseThrow().getBalance()).isEqualTo(20000);
		assertThat(walletRepository.findByMemberId(failedBuyerId).orElseThrow().getBalance()).isEqualTo(50000);
	}

	private Member saveMember(String loginId, String email, String name, boolean verified) {
		Member member = memberRepository.save(Member.create(loginId, "password", email, name, verified));
		member.assignDefaultNickname();
		return memberRepository.save(member);
	}

	private Product saveProduct(Member seller, ProductStatus status, BigDecimal price) {
		return productRepository.save(Product.create(
			"Purchase Product",
			"Purchase product description",
			ProductSaleType.REGULAR,
			19L,
			seller.getMemberId(),
			price,
			false,
			"Seoul Gangnam",
			null,
			status
		));
	}

	private long countPurchaseLedgers(Long memberId) {
		return walletLedgerRepository.findAllByMemberId(
				memberId,
				org.springframework.data.domain.Pageable.unpaged()
			).getContent().stream()
			.filter(ledger -> ledger.getType() == WalletLedgerType.PURCHASE)
			.count();
	}

	private int performConcurrentPurchase(
		Long productId,
		Member buyer,
		CountDownLatch ready,
		CountDownLatch start
	) throws Exception {
		ready.countDown();
		start.await(5, TimeUnit.SECONDS);

		return mockMvc.perform(post("/api/v1/products/{productId}/purchase", productId)
				.with(authentication(authenticatedMember(buyer)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson(UUID.randomUUID().toString())))
			.andReturn()
			.getResponse()
			.getStatus();
	}

	private String requestJson(String idempotencyKey) {
		return """
			{
			  "idempotencyKey": "%s"
			}
			""".formatted(idempotencyKey);
	}

	private UsernamePasswordAuthenticationToken authenticatedMember(Member member) {
		AuthenticatedMember principal = new AuthenticatedMember(member.getMemberId(), member.getLoginId(), "ROLE_USER");
		return new UsernamePasswordAuthenticationToken(
			principal,
			null,
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);
	}
}
