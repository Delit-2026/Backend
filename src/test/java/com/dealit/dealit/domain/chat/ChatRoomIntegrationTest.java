package com.dealit.dealit.domain.chat;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dealit.dealit.domain.chat.entity.ChatRoom;
import com.dealit.dealit.domain.chat.entity.ChatType;
import com.dealit.dealit.domain.chat.repository.ChatRoomRepository;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import com.dealit.dealit.domain.purchase.entity.Purchase;
import com.dealit.dealit.domain.purchase.repository.PurchaseRepository;
import com.dealit.dealit.global.security.AuthenticatedMember;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatRoomIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    private Member seller;
    private Member buyer;

    @BeforeEach
    void setUp() {
        purchaseRepository.deleteAll();
        chatRoomRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();

        seller = saveMember("chat-detail-seller", "chat-detail-seller@example.com", "Chat Detail Seller");
        buyer = saveMember("chat-detail-buyer", "chat-detail-buyer@example.com", "Chat Detail Buyer");
    }

    @Test
    @DisplayName("채팅방 테스트 컨텍스트 로드")
    void contextLoads() {
    }

    @Test
    @DisplayName("일반 판매 채팅방 상세에서 판매자는 PAID 상태에 발송 버튼이 활성화된다")
    void getGeneralChatRoomDetailReturnsEnabledSellerActionButtonWhenPaid() throws Exception {
        Product product = saveProduct(seller);
        ChatRoom room = saveChatRoom(product);
        Purchase purchase = savePaidPurchase(product);

        mockMvc.perform(get("/api/v1/chats/rooms/{roomId}/trade-info", room.getRoomId())
                        .with(authentication(authenticatedMember(seller))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(room.getRoomId()))
                .andExpect(jsonPath("$.purchaseId").value(purchase.getPurchaseId()))
                .andExpect(jsonPath("$.chatType").value("GENERAL"))
                .andExpect(jsonPath("$.currentUserRole").value("SELLER"))
                .andExpect(jsonPath("$.tradeStatus").value("PAID"))
                .andExpect(jsonPath("$.product.productId").value(product.getProductId()))
                .andExpect(jsonPath("$.opponent.userId").value(buyer.getMemberId()))
                .andExpect(jsonPath("$.actionButton.label").value("물건을 보냈어요"))
                .andExpect(jsonPath("$.actionButton.enabled").value(true))
                .andExpect(jsonPath("$.actionButton.actionType").value("SELLER_CONFIRM"))
                .andExpect(jsonPath("$.actionButton.disabledReason").doesNotExist());
    }

    @Test
    @DisplayName("일반 판매 채팅방 상세에서 구매자는 판매자 발송 전 수령 버튼이 비활성화된다")
    void getGeneralChatRoomDetailReturnsDisabledBuyerActionButtonBeforeShipping() throws Exception {
        Product product = saveProduct(seller);
        ChatRoom room = saveChatRoom(product);
        savePaidPurchase(product);

        mockMvc.perform(get("/api/v1/chats/rooms/{roomId}/trade-info", room.getRoomId())
                        .with(authentication(authenticatedMember(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentUserRole").value("BUYER"))
                .andExpect(jsonPath("$.tradeStatus").value("PAID"))
                .andExpect(jsonPath("$.opponent.userId").value(seller.getMemberId()))
                .andExpect(jsonPath("$.actionButton.label").value("물건을 받았어요"))
                .andExpect(jsonPath("$.actionButton.enabled").value(false))
                .andExpect(jsonPath("$.actionButton.actionType").value("BUYER_CONFIRM"))
                .andExpect(jsonPath("$.actionButton.disabledReason").value("SELLER_NOT_SHIPPED_YET"));
    }

    @Test
    @DisplayName("일반 판매 채팅방 상세에서 구매자는 SHIPPED 상태에 수령 버튼이 활성화된다")
    void getGeneralChatRoomDetailReturnsEnabledBuyerActionButtonWhenShipped() throws Exception {
        Product product = saveProduct(seller);
        ChatRoom room = saveChatRoom(product);
        Purchase purchase = Purchase.paid(
                product.getProductId(),
                buyer.getMemberId(),
                seller.getMemberId(),
                BigDecimal.valueOf(30000),
                UUID.randomUUID().toString()
        );
        purchase.markShipped();
        purchaseRepository.save(purchase);

        mockMvc.perform(get("/api/v1/chats/rooms/{roomId}/trade-info", room.getRoomId())
                        .with(authentication(authenticatedMember(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchaseId").value(purchase.getPurchaseId()))
                .andExpect(jsonPath("$.currentUserRole").value("BUYER"))
                .andExpect(jsonPath("$.tradeStatus").value("SHIPPED"))
                .andExpect(jsonPath("$.actionButton.label").value("물건을 받았어요"))
                .andExpect(jsonPath("$.actionButton.enabled").value(true))
                .andExpect(jsonPath("$.actionButton.actionType").value("BUYER_CONFIRM"))
                .andExpect(jsonPath("$.actionButton.disabledReason").doesNotExist());
    }

    private Member saveMember(String loginId, String email, String name) {
        Member member = memberRepository.save(Member.create(loginId, "password", email, name, true));
        member.assignDefaultNickname();
        return memberRepository.save(member);
    }

    private Product saveProduct(Member seller) {
        return productRepository.save(Product.create(
                "General Chat Product",
                "General chat product description",
                ProductSaleType.REGULAR,
                19L,
                seller.getMemberId(),
                BigDecimal.valueOf(30000),
                false,
                "Seoul Gangnam",
                null,
                ProductStatus.SOLD
        ));
    }

    private ChatRoom saveChatRoom(Product product) {
        return chatRoomRepository.save(ChatRoom.create(
                seller.getMemberId(),
                buyer.getMemberId(),
                product.getProductId(),
                ChatType.GENERAL
        ));
    }

    private Purchase savePaidPurchase(Product product) {
        return purchaseRepository.save(Purchase.paid(
                product.getProductId(),
                buyer.getMemberId(),
                seller.getMemberId(),
                BigDecimal.valueOf(30000),
                UUID.randomUUID().toString()
        ));
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
