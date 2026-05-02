package com.dealit.dealit.domain.product.dto;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "상품 상세 조회 응답")
public record ProductDetailResponse(
        @Schema(description = "상품 ID", example = "1001")
        Long productId,

        @Schema(description = "상품명", example = "아이폰 14 Pro")
        String name,

        @Schema(description = "상품 설명", example = "거의 새 상품입니다.")
        String description,

        @Schema(description = "판매 유형", example = "REGULAR", allowableValues = {"REGULAR", "AUCTION"})
        ProductSaleType saleType,

        @Schema(description = "카테고리 정보")
        CategoryResponse category,

        @Schema(description = "상품 이미지 URL 목록")
        List<String> imageUrls,

        @Schema(description = "상품 공통 상태", example = "ON_SALE")
        ProductStatus status,

        @Schema(description = "판매자 정보")
        SellerResponse seller,

        @Schema(description = "생성 시각", example = "2026-05-02T10:30:00+09:00")
        OffsetDateTime createdAt,

        @Schema(description = "수정 시각", example = "2026-05-02T11:00:00+09:00")
        OffsetDateTime updatedAt,

        @Schema(description = "일반 판매 상세 정보. 경매 상품이면 null")
        GeneralSaleResponse generalSale,

        @Schema(description = "경매 상세 정보. 일반 상품이면 null")
        AuctionResponse auction,

        @Schema(description = "채팅 가능 여부", example = "true")
        boolean canChat,

        @Schema(description = "입찰 가능 여부", example = "false")
        boolean canBid,

        @Schema(description = "구매 가능 여부", example = "true")
        boolean canPurchase,

        @Schema(description = "찜 가능 여부", example = "true")
        boolean canFavorite
) {

    public record CategoryResponse(
            @Schema(description = "카테고리 ID", example = "19")
            Long categoryId,

            @Schema(description = "카테고리 한글명", example = "전자기기")
            String nameKo,

            @Schema(description = "카테고리 영문명", example = "Digital/Electronics")
            String nameEn
    ) {
    }

    public record SellerResponse(
            @Schema(description = "판매자 회원 ID", example = "3")
            Long memberId,

            @Schema(description = "판매자 닉네임", example = "dealit-user-3")
            String nickname,

            @Schema(description = "거래 위치", example = "서울 강남구")
            String location
    ) {
    }

    public record GeneralSaleResponse(
            @Schema(description = "일반 판매 가격", example = "12000")
            BigDecimal price,

            @Schema(description = "조회수", example = "10")
            long viewCount,

            @Schema(description = "찜 수", example = "2")
            long favoriteCount,

            @Schema(description = "채팅 수", example = "1")
            long chatCount,

            @Schema(description = "일반 판매 상태", example = "ON_SALE")
            ProductStatus status
    ) {
    }

    public record AuctionResponse(
            @Schema(description = "경매 ID", example = "5")
            Long auctionId,

            @Schema(description = "경매 시작가", example = "70000")
            BigDecimal startPrice,

            @Schema(description = "현재가. 입찰 내역이 없으면 시작가와 동일", example = "70000")
            BigDecimal currentPrice,

            @Schema(description = "최소 다음 입찰가", example = "70700")
            BigDecimal minimumNextBidPrice,

      @Schema(description = "입찰 수", example = "0")
      long bidCount,

      @Schema(description = "조회수", example = "10")
      long viewCount,

      @Schema(description = "찜 수", example = "2")
      long favoriteCount,

      @Schema(description = "채팅 수", example = "1")
      long chatCount,

      @Schema(description = "경매 종료 시각", example = "2026-05-05T10:30:00+09:00")
      OffsetDateTime endAt,

      @Schema(description = "경매 상태", example = "AUCTION_LIVE")
            AuctionStatus status
    ) {
    }
}
