package com.dealit.dealit.domain.product;

import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.entity.ProductImage;
import com.dealit.dealit.domain.product.repository.ProductImageRepository;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import com.dealit.dealit.domain.review.entity.Review;
import com.dealit.dealit.domain.review.repository.ReviewRepository;
import com.dealit.dealit.global.security.AuthenticatedMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductDetailIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductImageRepository productImageRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	private Member seller;
	private Member viewer;

	@BeforeEach
	void setUp() {
		reviewRepository.deleteAll();
		productImageRepository.deleteAll();
		productRepository.deleteAll();
		memberRepository.deleteAll();

		seller = memberRepository.save(Member.create(
			"detail-seller",
			"password",
			"seller@example.com",
			null,
			"Detail Seller",
			true
		));
		seller.assignDefaultNickname();
		seller.updateProfile(seller.getName(), seller.getNickname(), "Detail seller bio.", seller.getProfileImage());
		seller.updateLocation("Seoul Gangnam");
		seller = memberRepository.save(seller);

		viewer = memberRepository.save(Member.create(
			"detail-viewer",
			"password",
			"viewer@example.com",
			null,
			"Detail Viewer",
			true
		));
		viewer.assignDefaultNickname();
		viewer.updateLocation("Seoul Mapo");
		viewer = memberRepository.save(viewer);
	}

	@Test
	@DisplayName("일반 상품 상세 조회에 성공하면 generalSale을 반환한다")
	void getRegularProductDetailSuccess() throws Exception {
		Product product = saveRegularProduct();
		attachImage(product, "/uploads/product/images/regular-1.jpg", 1);

		mockMvc.perform(get("/api/v1/products/{productId}", product.getProductId())
				.with(authentication(authenticatedMember(viewer))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.productId").value(product.getProductId()))
			.andExpect(jsonPath("$.name").value("Regular Product"))
			.andExpect(jsonPath("$.description").value("Regular product description"))
			.andExpect(jsonPath("$.saleType").value("REGULAR"))
			.andExpect(jsonPath("$.category.categoryId").value(19))
			.andExpect(jsonPath("$.imageUrls[0]").value(startsWith("http://localhost:8080/uploads/product/images/regular-1.jpg")))
			.andExpect(jsonPath("$.status").value("ON_SALE"))
			.andExpect(jsonPath("$.seller.memberId").value(seller.getMemberId()))
			.andExpect(jsonPath("$.seller.nickname").value(seller.getNickname()))
			.andExpect(jsonPath("$.seller.bio").value("Detail seller bio."))
			.andExpect(jsonPath("$.seller.location").value("Seoul Gangnam"))
			.andExpect(jsonPath("$.seller.rating").value(0.0))
			.andExpect(jsonPath("$.generalSale.price").value(12000))
			.andExpect(jsonPath("$.generalSale.viewCount").value(1))
			.andExpect(jsonPath("$.generalSale.favoriteCount").value(0))
			.andExpect(jsonPath("$.generalSale.chatCount").value(0))
			.andExpect(jsonPath("$.generalSale.status").value("ON_SALE"))
			.andExpect(jsonPath("$.canChat").value(true))
			.andExpect(jsonPath("$.canPurchase").value(true))
			.andExpect(jsonPath("$.purchaseBlockedReason").doesNotExist())
			.andExpect(jsonPath("$.canFavorite").value(true));
	}

	@Test
	@DisplayName("상품 상세 조회 시 판매자가 받은 리뷰 평균 평점을 반환한다")
	void getRegularProductDetailReturnsSellerRating() throws Exception {
		Product product = saveRegularProduct();
		Member anotherBuyer = memberRepository.save(Member.create(
			"detail-another-buyer",
			"password",
			"another-buyer@example.com",
			null,
			"Detail Another Buyer",
			true
		));
		reviewRepository.save(Review.create(
			viewer.getMemberId(),
			seller.getMemberId(),
			product.getProductId(),
			null,
			BigDecimal.valueOf(5.0),
			"Great transaction."
		));
		reviewRepository.save(Review.create(
			anotherBuyer.getMemberId(),
			seller.getMemberId(),
			product.getProductId(),
			null,
			BigDecimal.valueOf(4.0),
			"Good transaction."
		));

		mockMvc.perform(get("/api/v1/products/{productId}", product.getProductId())
				.with(authentication(authenticatedMember(viewer))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.seller.rating").value(4.5));
	}

	@Test
	@DisplayName("이메일 미인증 사용자가 상품 상세를 조회하면 구매 불가 사유를 반환한다")
	void getProductDetailReturnsEmailNotVerifiedReason() throws Exception {
		Member unverifiedViewer = memberRepository.save(Member.create(
			"detail-unverified-viewer",
			"password",
			"unverified-viewer@example.com",
			null,
			"Detail Unverified Viewer",
			false
		));
		unverifiedViewer.assignDefaultNickname();
		unverifiedViewer = memberRepository.save(unverifiedViewer);
		Product product = saveRegularProduct();

		mockMvc.perform(get("/api/v1/products/{productId}", product.getProductId())
				.with(authentication(authenticatedMember(unverifiedViewer))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.canPurchase").value(false))
			.andExpect(jsonPath("$.purchaseBlockedReason").value("EMAIL_NOT_VERIFIED"));
	}

	@Test
	@DisplayName("본인 상품 상세를 조회하면 구매 불가 사유를 반환한다")
	void getProductDetailReturnsOwnProductReason() throws Exception {
		Product product = saveRegularProduct();

		mockMvc.perform(get("/api/v1/products/{productId}", product.getProductId())
				.with(authentication(authenticatedMember(seller))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.canPurchase").value(false))
			.andExpect(jsonPath("$.purchaseBlockedReason").value("OWN_PRODUCT"));
	}

	@Test
	@DisplayName("판매 중이 아닌 상품 상세를 조회하면 구매 불가 사유를 반환한다")
	void getProductDetailReturnsProductNotPurchasableReason() throws Exception {
		Product product = productRepository.save(Product.create(
			"Sold Product",
			"Sold product description",
			ProductSaleType.REGULAR,
			19L,
			seller.getMemberId(),
			BigDecimal.valueOf(12000),
			false,
			"Seoul Gangnam",
			null,
			ProductStatus.SOLD
		));

		mockMvc.perform(get("/api/v1/products/{productId}", product.getProductId())
				.with(authentication(authenticatedMember(viewer))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.canPurchase").value(false))
			.andExpect(jsonPath("$.purchaseBlockedReason").value("PRODUCT_NOT_PURCHASABLE"));
	}

	@Test
	@DisplayName("상품 상세 조회에 성공하면 조회수가 증가한다")
	void getProductDetailIncreasesViewCount() throws Exception {
		Product product = saveRegularProduct();

		mockMvc.perform(get("/api/v1/products/{productId}", product.getProductId())
				.with(authentication(authenticatedMember(viewer))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.generalSale.viewCount").value(1));

		mockMvc.perform(get("/api/v1/products/{productId}", product.getProductId())
				.with(authentication(authenticatedMember(viewer))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.generalSale.viewCount").value(2));
	}

	@Test
	@DisplayName("경매 상품은 일반 상품 상세 조회 API에서 조회하지 않는다")
	void getProductDetailFailsWhenProductIsAuction() throws Exception {
		Product product = saveAuctionProduct();

		mockMvc.perform(get("/api/v1/products/{productId}", product.getProductId())
				.with(authentication(authenticatedMember(viewer))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	@DisplayName("존재하지 않는 상품을 조회하면 404 PRODUCT_NOT_FOUND를 반환한다")
	void getProductDetailFailsWhenProductNotFound() throws Exception {
		mockMvc.perform(get("/api/v1/products/{productId}", 999999L)
				.with(authentication(authenticatedMember(viewer))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	@DisplayName("비로그인 사용자는 상품 상세를 조회할 수 없다")
	void getProductDetailRequiresAuthentication() throws Exception {
		Product product = saveRegularProduct();

		mockMvc.perform(get("/api/v1/products/{productId}", product.getProductId()))
			.andExpect(status().isUnauthorized());
	}

	private Product saveRegularProduct() {
		return productRepository.save(Product.create(
			"Regular Product",
			"Regular product description",
			ProductSaleType.REGULAR,
			19L,
			seller.getMemberId(),
			BigDecimal.valueOf(12000),
			false,
			"Seoul Gangnam",
			null,
			ProductStatus.ON_SALE
		));
	}

	private Product saveAuctionProduct() {
		return productRepository.save(Product.create(
			"Auction Product",
			"Auction product description",
			ProductSaleType.AUCTION,
			19L,
			seller.getMemberId(),
			BigDecimal.valueOf(70000),
			false,
			"Seoul Gangnam",
			null,
			ProductStatus.ON_SALE
		));
	}

	private void attachImage(Product product, String imageUrl, int sortOrder) {
		ProductImage image = productImageRepository.save(
			ProductImage.createTemporary(imageUrl, "test-image.jpg", seller.getMemberId())
		);
		product.attachImage(image, sortOrder);
		productImageRepository.save(image);
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
