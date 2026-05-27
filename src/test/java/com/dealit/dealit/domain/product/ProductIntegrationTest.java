package com.dealit.dealit.domain.product;

import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.notification.repository.FcmTokenRepository;
import com.dealit.dealit.domain.product.entity.ProductImage;
import com.dealit.dealit.domain.product.repository.ProductDraftRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

	@Autowired
	private FcmTokenRepository fcmTokenRepository;

	@Value("${app.images.storage-root}")
	private String imageStorageRoot;

	private ProductImage uploadedImage;
	private Member member;
	private Member otherMember;

	@BeforeEach
	void setUp() throws IOException {
		productImageRepository.deleteAll();
		productRepository.deleteAll();
		productDraftRepository.deleteAll();
		fcmTokenRepository.deleteAll();
		memberRepository.deleteAll();
		deleteStoredImages();

		member = memberRepository.save(Member.create("product-user", "password", "product@example.com", null, "상품회원"));
		member.assignDefaultNickname();
		member.verifyEmail();
		member.updateLocation("서울 강남구");
		member = memberRepository.save(member);

		otherMember = memberRepository.save(Member.create("product-user-2", "password", "product2@example.com", null, "다른회원"));
		otherMember.assignDefaultNickname();
		otherMember.verifyEmail();
		otherMember.updateLocation("서울 서초구");
		otherMember = memberRepository.save(otherMember);

		uploadedImage = productImageRepository.save(
			ProductImage.createTemporary("/uploads/product/images/test-image.jpg", "test-image.jpg", member.getMemberId())
		);
	}

	@AfterEach
	void tearDown() throws IOException {
		deleteStoredImages();
	}

	@Test
	@DisplayName("일반 상품 이미지 업로드 응답은 접근 가능한 공개 URL을 반환한다")
	void uploadProductImageReturnsAccessibleUrl() throws Exception {
		byte[] imageBytes = "test-image".getBytes();
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"regular-product.png",
			MediaType.IMAGE_PNG_VALUE,
			imageBytes
		);

		mockMvc.perform(
				multipart("/api/v1/products/image")
					.file(file)
					.with(authentication(authenticatedMember()))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imageId").isNumber())
			.andExpect(jsonPath("$.imageUrl").value(startsWith("http://localhost:8080/uploads/product/images/")))
			.andExpect(jsonPath("$.imageUrl").value(org.hamcrest.Matchers.endsWith(".png")));

		ProductImage savedImage = productImageRepository.findAll().stream()
			.filter(image -> image.getOriginalFileName().equals("regular-product.png"))
			.findFirst()
			.orElseThrow();

		mockMvc.perform(get(savedImage.getImageUrl()))
			.andExpect(status().isOk())
			.andExpect(content().bytes(imageBytes));
	}

	@Test
	@DisplayName("비로그인 사용자는 일반 상품 이미지를 업로드할 수 없다")
	void uploadProductImageRequiresAuthentication() throws Exception {
		byte[] imageBytes = "test-image".getBytes();
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"regular-product.png",
			MediaType.IMAGE_PNG_VALUE,
			imageBytes
		);

		mockMvc.perform(multipart("/api/v1/products/image").file(file))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("일반 상품 등록 성공 시 ON_SALE 상태와 가격 정보를 반환한다")
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
					      "imageUrl": "http://localhost:8080/uploads/product/images/test-image.jpg",
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
	@DisplayName("일반 상품 수정 시 현재 상품에 연결된 기존 이미지는 그대로 유지할 수 있다")
	void updateProductKeepsExistingImage() throws Exception {
		String response = mockMvc.perform(post("/api/v1/products")
				.with(authentication(authenticatedMember()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "수정 전 상품명",
					  "description": "수정 전 설명",
					  "saleType": "REGULAR",
					  "categoryId": 19,
					  "price": 12000,
					  "allowOffer": false,
					  "images": [
					    {
					      "imageId": %d,
					      "imageUrl": "http://localhost:8080/uploads/product/images/test-image.jpg",
					      "sortOrder": 1
					    }
					  ],
					  "location": "서울 강남구",
					  "draftId": null
					}
					""".formatted(uploadedImage.getImageId())))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();

		Number productId = com.jayway.jsonpath.JsonPath.read(response, "$.productId");

		mockMvc.perform(patch("/api/v1/products/{productId}", productId.longValue())
				.with(authentication(authenticatedMember()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "수정된 상품명",
					  "description": "수정된 설명",
					  "categoryId": 19,
					  "price": 30000,
					  "allowOffer": false,
					  "location": "서울 강남구",
					  "images": [
					    {
					      "imageId": %d,
					      "imageUrl": "http://localhost:8080/uploads/product/images/test-image.jpg",
					      "sortOrder": 1
					    }
					  ]
					}
					""".formatted(uploadedImage.getImageId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.productId").value(productId))
			.andExpect(jsonPath("$.name").value("수정된 상품명"))
			.andExpect(jsonPath("$.description").value("수정된 설명"))
			.andExpect(jsonPath("$.price").value(30000))
			.andExpect(jsonPath("$.images", hasSize(1)))
			.andExpect(jsonPath("$.images[0].imageId").value(uploadedImage.getImageId()));
	}

	@Test
	@DisplayName("이메일 미인증 회원은 일반 상품을 등록할 수 없다")
	void createProductFailsWhenEmailNotVerified() throws Exception {
		Member unverifiedMember = memberRepository.save(
			Member.create("unverified-product-user", "password", "unverified-product@example.com", null, "미인증회원")
		);
		unverifiedMember.assignDefaultNickname();
		unverifiedMember.updateLocation("서울 강남구");
		unverifiedMember = memberRepository.save(unverifiedMember);
		ProductImage unverifiedImage = productImageRepository.save(
			ProductImage.createTemporary("/uploads/product/images/unverified-image.jpg", "unverified-image.jpg", unverifiedMember.getMemberId())
		);

		mockMvc.perform(post("/api/v1/products")
				.with(authentication(authenticatedMember(unverifiedMember)))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "미인증 상품",
					  "description": "이메일 인증 전 등록 시도",
					  "saleType": "REGULAR",
					  "categoryId": 19,
					  "price": 12000,
					  "allowOffer": false,
					  "images": [
					    {
					      "imageId": %d,
					      "imageUrl": "http://localhost:8080/uploads/product/images/unverified-image.jpg",
					      "sortOrder": 1
					    }
					  ],
					  "location": "서울 강남구",
					  "draftId": null
					}
					""".formatted(unverifiedImage.getImageId())))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
	}

	@Test
	@DisplayName("일반 상품 임시저장은 기존 규칙대로 저장된다")
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
					      "imageUrl": "http://localhost:8080/uploads/product/images/test-image.jpg",
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
	@DisplayName("일반 상품 카테고리 목록 조회는 계층 구조를 반환한다")
	void getProductCategoriesReturnsHierarchy() throws Exception {
		mockMvc.perform(get("/api/v1/products/categories"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(3)));
	}

	@Test
	@DisplayName("일반 상품 이미지 삭제는 imageId 기준으로 처리된다")
	void deleteProductImageSuccess() throws Exception {
		mockMvc.perform(
				delete("/api/v1/products/image/{imageId}", uploadedImage.getImageId())
					.with(authentication(authenticatedMember()))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imageId").value(uploadedImage.getImageId()))
			.andExpect(jsonPath("$.deleted").value(true));
	}

	@Test
	@DisplayName("다른 사용자는 업로드하지 않은 일반 상품 이미지를 삭제할 수 없다")
	void deleteProductImageFailsWhenNotOwner() throws Exception {
		mockMvc.perform(
				delete("/api/v1/products/image/{imageId}", uploadedImage.getImageId())
					.with(authentication(authenticatedMember(otherMember)))
			)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("PRODUCT_ACCESS_DENIED"));
	}

	@Test
	@DisplayName("비로그인 사용자는 일반 상품 이미지를 삭제할 수 없다")
	void deleteProductImageRequiresAuthentication() throws Exception {
		mockMvc.perform(delete("/api/v1/products/image/{imageId}", uploadedImage.getImageId()))
			.andExpect(status().isUnauthorized());
	}

	private UsernamePasswordAuthenticationToken authenticatedMember() {
		return authenticatedMember(member);
	}

	private UsernamePasswordAuthenticationToken authenticatedMember(Member member) {
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
