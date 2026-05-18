package com.dealit.dealit.domain.product;

import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.entity.MemberInterestCategory;
import com.dealit.dealit.domain.member.repository.MemberInterestCategoryRepository;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import com.dealit.dealit.global.security.AuthenticatedMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HotListProductIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberInterestCategoryRepository memberInterestCategoryRepository;

	private Member member;

	@BeforeEach
	void setUp() {
		memberInterestCategoryRepository.deleteAll();
		productRepository.deleteAll();
		memberRepository.deleteAll();

		member = memberRepository.save(Member.create(
			"hot-list-user",
			"password",
			"hot-list@example.com",
			null,
			"hot-list-user"
		));
		member.assignDefaultNickname();
		member.verifyEmail();
		member.updateLocation("Seoul");
		member = memberRepository.save(member);
	}

	@Test
	@DisplayName("hot-list uses the current member's interest categories")
	void getHotListProductsUsesMemberInterestCategories() throws Exception {
		memberInterestCategoryRepository.save(MemberInterestCategory.create(member.getMemberId(), 1L));

		saveProductWithCounts("interest electronics product", 19L, 5, 2, 2);
		saveProductWithCounts("non interest fashion product", 28L, 100, 20, 1);

		mockMvc.perform(get("/api/v1/products/hot-list")
				.with(authentication(authenticatedMember()))
				.param("size", "8"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].name").value("interest electronics product"))
			.andExpect(jsonPath("$.content[0].rank").value(1));
	}

	@Test
	@DisplayName("hot-list falls back to all products when the member has no interest categories")
	void getHotListProductsFallsBackToAllProductsWhenNoInterestCategories() throws Exception {
		saveProductWithCounts("low score product", 19L, 2, 0, 1);
		saveProductWithCounts("high score product", 28L, 10, 5, 1);

		mockMvc.perform(get("/api/v1/products/hot-list")
				.with(authentication(authenticatedMember()))
				.param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(2)))
			.andExpect(jsonPath("$.content[0].name").value("high score product"))
			.andExpect(jsonPath("$.content[0].rank").value(1))
			.andExpect(jsonPath("$.content[1].name").value("low score product"))
			.andExpect(jsonPath("$.content[1].rank").value(2));
	}

	private Product saveProductWithCounts(String name, Long categoryId, int viewCount, int favoriteCount, int elapsedHours) {
		Product product = productRepository.save(Product.create(
			name,
			"hot-list test product",
			ProductSaleType.REGULAR,
			categoryId,
			member.getMemberId(),
			BigDecimal.valueOf(10000),
			false,
			"Seoul",
			null,
			ProductStatus.ON_SALE
		));
		ReflectionTestUtils.setField(product, "createdAt", LocalDateTime.now().minusHours(elapsedHours));
		ReflectionTestUtils.setField(product, "updatedAt", LocalDateTime.now().minusHours(elapsedHours));
		for (int count = 0; count < viewCount; count++) {
			product.increaseViewCount();
		}
		for (int count = 0; count < favoriteCount; count++) {
			product.increaseFavoriteCount();
		}
		return productRepository.save(product);
	}

	private UsernamePasswordAuthenticationToken authenticatedMember() {
		AuthenticatedMember principal = new AuthenticatedMember(member.getMemberId(), member.getLoginId(), "ROLE_USER");
		return new UsernamePasswordAuthenticationToken(
			principal,
			null,
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);
	}
}
