package com.dealit.dealit.domain.auction;

import com.dealit.dealit.domain.auction.entity.AuctionProductImage;
import com.dealit.dealit.domain.auction.repository.AuctionProductImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuctionIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuctionProductImageRepository auctionProductImageRepository;

	private AuctionProductImage uploadedImage;

	@BeforeEach
	void setUp() {
		auctionProductImageRepository.deleteAll();
		uploadedImage = auctionProductImageRepository.saveAndFlush(
			AuctionProductImage.createTemporary(
				"https://cdn.dealit.local/auction/images/test-image.jpg",
				"test-image.jpg"
			)
		);
	}

	@Test
	@DisplayName("일반 판매 상품 등록에 성공하면 ON_SALE 상태를 반환한다")
	void createRegularAuctionSuccess() throws Exception {
		mockMvc.perform(post("/api/v1/auction")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "MacBook Air M2",
					  "description": "Lightly used and includes charger.",
					  "saleType": "REGULAR",
					  "categoryId": 12,
					  "price": 1350000,
					  "startPrice": null,
					  "auctionEndAt": null,
					  "allowOffer": false,
					  "images": [
					    {
					      "imageId": %d,
					      "imageUrl": "https://cdn.dealit.local/auction/images/test-image.jpg",
					      "sortOrder": 1
					    }
					  ],
					  "location": "Seoul",
					  "draftId": null
					}
					""".formatted(uploadedImage.getImageId())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.productId").isNumber())
			.andExpect(jsonPath("$.saleType").value("REGULAR"))
			.andExpect(jsonPath("$.status").value("ON_SALE"));
	}

	@Test
	@DisplayName("경매 판매에서 시작가가 없으면 비즈니스 검증 오류를 반환한다")
	void createAuctionFailsWhenStartPriceMissing() throws Exception {
		mockMvc.perform(post("/api/v1/auction")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "Rolex Datejust",
					  "description": "Authentic watch in good condition.",
					  "saleType": "AUCTION",
					  "categoryId": 44,
					  "price": null,
					  "startPrice": null,
					  "auctionEndAt": "2099-04-15T12:00:00Z",
					  "allowOffer": false,
					  "images": [
					    {
					      "imageId": %d,
					      "imageUrl": "https://cdn.dealit.local/auction/images/test-image.jpg",
					      "sortOrder": 1
					    }
					  ],
					  "location": "Busan",
					  "draftId": null
					}
					""".formatted(uploadedImage.getImageId())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_AUCTION_REQUEST"))
			.andExpect(jsonPath("$.message").value("startPrice is required when saleType is AUCTION."));
	}
}
