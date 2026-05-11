package com.dealit.dealit.domain.wishlist;

import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.notification.repository.InAppNotificationRepository;
import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import com.dealit.dealit.domain.wishlist.service.WishlistService;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WishlistNotificationIntegrationTest {

	@Autowired
	private WishlistService wishlistService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private InAppNotificationRepository notificationRepository;

	@Test
	@DisplayName("상품을 찜하면 판매자 알림 센터에 알림이 쌓인다")
	void addWishlistCreatesNotification() {
		Member seller = createMember("seller");
		Member buyer = createMember("buyer");
		Product product = productRepository.save(Product.create(
			"테스트 상품",
			"테스트 상품 설명",
			ProductSaleType.REGULAR,
			16L,
			seller.getMemberId(),
			BigDecimal.valueOf(10000),
			true,
			"서울",
			null,
			ProductStatus.ON_SALE
		));

		wishlistService.addWishlist(buyer.getMemberId(), product.getProductId());

		assertThat(notificationRepository.countByMemberMemberIdAndReadAtIsNullAndDeletedAtIsNull(seller.getMemberId()))
			.isEqualTo(1);
	}

	private Member createMember(String prefix) {
		String suffix = Long.toUnsignedString(System.nanoTime(), 36);
		String loginId = prefix + "_" + suffix;
		String email = loginId + "@t.com";

		return memberRepository.saveAndFlush(Member.create(
			loginId,
			"encoded-password",
			email,
			"010-0000-0000",
			prefix
		));
	}
}
