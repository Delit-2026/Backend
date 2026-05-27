package com.dealit.dealit.domain.product;

import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PopularProductIntegrationTest {

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private MemberRepository memberRepository;

	private Member member;

	@BeforeEach
	void setUp() {
		productRepository.deleteAll();
		memberRepository.deleteAll();

		member = memberRepository.save(Member.create(
			"popular-user",
			"password",
			"popular@example.com",
			null,
			"인기회원"
		));
		member.assignDefaultNickname();
		member.verifyEmail();
		member.updateLocation("서울 강남구");
		member = memberRepository.save(member);
	}

	@Test
	@DisplayName("실시간 인기 일반 상품 목록은 점수 순으로 반환하고 경매 상품은 제외한다")
	void getPopularProductsReturnsRegularProductsSortedByScore() throws Exception {
		Product lowScoreProduct = productRepository.save(Product.create(
			"오래된 인기 상품",
			"낮은 점수 상품",
			ProductSaleType.REGULAR,
			19L,
			member.getMemberId(),
			BigDecimal.valueOf(10000),
			false,
			"서울 강남구",
			null,
			ProductStatus.ON_SALE
		));
		LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);
		ReflectionTestUtils.setField(lowScoreProduct, "createdAt", now.minusHours(10));
		ReflectionTestUtils.setField(lowScoreProduct, "updatedAt", now.minusHours(10));
		for (int count = 0; count < 10; count++) {
			lowScoreProduct.increaseViewCount();
		}
		productRepository.save(lowScoreProduct);

		Product highScoreProduct = productRepository.save(Product.create(
			"최근 인기 상품",
			"높은 점수 상품",
			ProductSaleType.REGULAR,
			19L,
			member.getMemberId(),
			BigDecimal.valueOf(20000),
			false,
			"서울 마포구",
			null,
			ProductStatus.ON_SALE
		));
		ReflectionTestUtils.setField(highScoreProduct, "createdAt", now.minusHours(2));
		ReflectionTestUtils.setField(highScoreProduct, "updatedAt", now.minusHours(2));
		for (int count = 0; count < 12; count++) {
			highScoreProduct.increaseViewCount();
		}
		productRepository.save(highScoreProduct);

		Product auctionProduct = productRepository.save(Product.create(
			"경매 상품",
			"제외되어야 하는 상품",
			ProductSaleType.AUCTION,
			19L,
			member.getMemberId(),
			BigDecimal.valueOf(30000),
			false,
			"서울 서초구",
			null,
			ProductStatus.ON_SALE
		));
		ReflectionTestUtils.setField(auctionProduct, "createdAt", now.minusHours(1));
		auctionProduct.increaseViewCount();
		productRepository.save(auctionProduct);

		mockMvc.perform(get("/api/v1/products/popular").param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(2)))
			.andExpect(jsonPath("$.content[0].name").value("최근 인기 상품"))
			.andExpect(jsonPath("$.content[0].popularScore").value(6.0))
			.andExpect(jsonPath("$.content[1].name").value("오래된 인기 상품"));
	}
}
