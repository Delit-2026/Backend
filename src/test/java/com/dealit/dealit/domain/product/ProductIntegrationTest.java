package com.dealit.dealit.domain.product;

import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.product.repository.ProductDraftRepository;
import com.dealit.dealit.domain.product.entity.ProductImage;
import com.dealit.dealit.domain.product.repository.ProductImageRepository;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import com.dealit.dealit.global.security.AuthenticatedMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@SpringBootTest
@AutoConfigureMockMvc
class ProductIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductImageRepository productImageRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductDraftRepository productDraftRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Value("${app.images.storage-root}")
	private String imageStorageRoot;

	private ProductImage uploadedImage;
	private Member member;

	@BeforeEach
	void setUp() throws IOException {
		productImageRepository.deleteAll();
		productRepository.deleteAll();
		productDraftRepository.deleteAll();
		memberRepository.deleteAll();
		deleteStoredImages();
		member = memberRepository.save(Member.create("product-user", "password", "product@example.com", null, "상품회원"));
		member.assignDefaultNickname();
		member.updateLocation("서울 강남구");
		uploadedImage = productImageRepository.save(
			ProductImage.createTemporary("/product/images/test-image.jpg", "test-image.jpg")
		);
	}

	@AfterEach
	void tearDown() throws IOException {
		deleteStoredImages();
	}

	@Test
	@DisplayName("일반 상품 이미지 업로드 응답은 브라우저에서 접근 가능한 절대 URL을 반환하고 정적 서빙된다")
	void uploadProductImageReturnsAccessibleUrl() throws Exception {
		byte[] imageBytes = "test-image".getBytes();
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"regular-product.png",
			MediaType.IMAGE_PNG_VALUE,
			imageBytes
		);

		mockMvc.perform(multipart("/api/v1/products/image").file(file))
			.with(authentication(authenticatedMember()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imageId").isNumber())
			.andExpect(jsonPath("$.imageUrl").value(startsWith("http://localhost:8080/product/images/")))
			.andExpect(jsonPath("$.imageUrl").value(containsString("regular-product.png")));

		ProductImage savedImage = productImageRepository.findAll().stream()
			.filter(image -> image.getOriginalFileName().equals("regular-product.png"))
			.findFirst()
			.orElseThrow();

		mockMvc.perform(get(savedImage.getImageUrl()))
			.andExpect(status().isOk())
			.andExpect(content().bytes(imageBytes));
	}

	@Test
	@DisplayName("일반 상품 등록에 성공하면 generalSale.price 와 ON_SALE 상태를 반환한다")
	void createProductSuccess() throws Exception {
		mockMvc.perform(post("/api/v1/products")
				.with(authentication(authenticatedMember()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "일반 판매 상품명",
					  "description": "상품 설명",
					  "saleType": "REGULAR",
					  "categoryId": 19,
					  "price": 12000,
					  "allowOffer": false,
					  "images": [
					    {
					      "imageId": %d,
					      "imageUrl": "http://localhost:8080/product/images/test-image.jpg",
					      "sortOrder": 1
					    }
					  ],
					  "location": "부산 해운대구",
					  "draftId": null
					}
					""".formatted(uploadedImage.getImageId())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.productId").isNumber())
			.andExpect(jsonPath("$.saleType").value("REGULAR"))
			.andExpect(jsonPath("$.status").value("ON_SALE"))
			.andExpect(jsonPath("$.auction").value(nullValue()))
			.andExpect(jsonPath("$.generalSale.price").value(12000));
	}

	@Test
	@DisplayName("일반 상품 draft 저장은 동일한 규칙으로 처리한다")
	void saveProductDraftSuccess() throws Exception {
		mockMvc.perform(post("/api/v1/products/draft")
				.with(authentication(authenticatedMember()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "일반 판매 상품명",
					  "description": "상품 설명",
					  "saleType": "REGULAR",
					  "categoryId": 19,
					  "price": 12000,
					  "allowOffer": false,
					  "images": [
					    {
					      "imageId": %d,
					      "imageUrl": "http://localhost:8080/product/images/test-image.jpg",
					      "sortOrder": 1
					    }
					  ],
					  "location": "부산 해운대구",
					  "draftId": null
					}
					""".formatted(uploadedImage.getImageId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.draftId").isNumber())
			.andExpect(jsonPath("$.savedAt").exists());
	}

	@Test
	@DisplayName("일반 상품 카테고리 목록 조회는 계층형 트리 구조를 반환한다")
	void getProductCategoriesReturnsHierarchy() throws Exception {
		mockMvc.perform(get("/api/v1/products/categories"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(3)))
			.andExpect(jsonPath("$[0].nameKo").value("전자기기"))
			.andExpect(jsonPath("$[0].children", hasSize(4)));
	}

	@Test
	@DisplayName("일반 상품 이미지 삭제는 imageId 기준으로 처리한다")
	void deleteProductImageSuccess() throws Exception {
		mockMvc.perform(delete("/api/v1/products/image/{imageId}", uploadedImage.getImageId()))
			.with(authentication(authenticatedMember()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imageId").value(uploadedImage.getImageId()))
			.andExpect(jsonPath("$.deleted").value(true));
	}

	private UsernamePasswordAuthenticationToken authenticatedMember() {
		AuthenticatedMember principal = new AuthenticatedMember(member.getMemberId(), member.getLoginId(), "ROLE_USER");
		return new UsernamePasswordAuthenticationToken(
			principal,
			null,
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);
	}

	private void deleteStoredImages() throws IOException {
		Path storageRoot = Path.of(imageStorageRoot);
		if (!Files.exists(storageRoot)) {
			return;
		}

		try (var paths = Files.walk(storageRoot)) {
			paths.sorted(Comparator.reverseOrder())
				.forEach(path -> {
					try {
						Files.deleteIfExists(path);
					} catch (IOException exception) {
						throw new RuntimeException(exception);
					}
				});
		}
	}
}
