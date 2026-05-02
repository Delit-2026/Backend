package com.dealit.dealit.domain.product.service;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.entity.Auction;
import com.dealit.dealit.domain.auction.entity.Category;
import com.dealit.dealit.domain.auction.repository.AuctionRepository;
import com.dealit.dealit.domain.auction.repository.CategoryRepository;
import com.dealit.dealit.domain.auth.exception.InvalidCredentialsException;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;
import com.dealit.dealit.domain.product.dto.ProductDetailResponse;
import com.dealit.dealit.domain.product.entity.Product;
import com.dealit.dealit.domain.product.entity.ProductImage;
import com.dealit.dealit.domain.product.exception.ProductNotFoundException;
import com.dealit.dealit.domain.product.repository.ProductRepository;
import com.dealit.dealit.global.service.ImageUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductDetailService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final ProductRepository productRepository;
    private final AuctionRepository auctionRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;
    private final ImageUrlService imageUrlService;

    @Transactional
    public ProductDetailResponse getProductDetail(Long memberId, Long productId) {
        Member viewer = loadActiveMember(memberId);
        Product product = productRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ProductNotFoundException("존재하지 않는 상품입니다."));

        product.increaseViewCount();

        Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
        Member seller = memberRepository.findByMemberIdAndDeletedAtIsNull(product.getMemberId())
                .orElseThrow(() -> new ProductNotFoundException("상품 판매자 정보를 찾을 수 없습니다."));

        boolean owner = seller.getMemberId().equals(viewer.getMemberId());

        return new ProductDetailResponse(
                product.getProductId(),
                product.getName(),
                product.getDescription(),
                product.getSaleType(),
                toCategoryResponse(category, product.getCategoryId()),
                toImageUrls(product),
                product.getStatus(),
                toSellerResponse(seller, product),
                toSeoulOffsetDateTime(product.getCreatedAt()),
                toSeoulOffsetDateTime(product.getUpdatedAt() == null ? product.getCreatedAt() : product.getUpdatedAt()),
                toGeneralSaleResponse(product),
                toAuctionResponse(product),
                canChat(product, owner),
                canBid(product, owner),
                canPurchase(product, owner),
                canFavorite(product, owner)
        );
    }

    private Member loadActiveMember(Long memberId) {
        return memberRepository.findByMemberIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new InvalidCredentialsException("존재하지 않는 회원입니다."));
    }

    private ProductDetailResponse.CategoryResponse toCategoryResponse(Category category, Long categoryId) {
        if (category == null) {
            return new ProductDetailResponse.CategoryResponse(categoryId, "", "");
        }

        return new ProductDetailResponse.CategoryResponse(
                category.getId(),
                category.getNameKo(),
                category.getNameEn()
        );
    }

    private ProductDetailResponse.SellerResponse toSellerResponse(Member seller, Product product) {
        return new ProductDetailResponse.SellerResponse(
                seller.getMemberId(),
                seller.getNickname(),
                product.getLocation()
        );
    }

    private List<String> toImageUrls(Product product) {
        return product.getImages().stream()
                .filter(image -> image.getDeletedAt() == null)
                .filter(image -> image.getImageUrl() != null && !image.getImageUrl().isBlank())
                .sorted(Comparator.comparing(
                        ProductImage::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo)
                ))
                .map(image -> imageUrlService.toPublicUrl(image.getImageUrl()))
                .toList();
    }

    private ProductDetailResponse.GeneralSaleResponse toGeneralSaleResponse(Product product) {
        if (product.getSaleType() != ProductSaleType.REGULAR) {
            return null;
        }

        return new ProductDetailResponse.GeneralSaleResponse(
                product.getPrice(),
                product.getViewCount(),
                product.getFavoriteCount(),
                product.getChatCount(),
                product.getStatus()
        );
    }

    private ProductDetailResponse.AuctionResponse toAuctionResponse(Product product) {
        if (product.getSaleType() != ProductSaleType.AUCTION) {
            return null;
        }

        Auction auction = auctionRepository.findByProductProductIdAndDeletedAtIsNullAndProductDeletedAtIsNull(product.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("경매 상품 정보를 찾을 수 없습니다."));

        BigDecimal currentPrice = resolveCurrentPrice(auction);

        return new ProductDetailResponse.AuctionResponse(
                auction.getAuctionId(),
            auction.getStartPrice(),
            currentPrice,
            resolveMinimumNextBidPrice(currentPrice),
            getBidCount(auction),
            product.getViewCount(),
            product.getFavoriteCount(),
            product.getChatCount(),
            toSeoulOffsetDateTime(auction.getAuctionEndAt()),
            auction.getStatus()
      );
   }

    private BigDecimal resolveCurrentPrice(Auction auction) {
        if (auction.getCurrentPrice() == null) {
            return auction.getStartPrice();
        }

        return auction.getCurrentPrice();
    }

    private BigDecimal resolveMinimumNextBidPrice(BigDecimal currentPrice) {
        BigDecimal increaseAmount = currentPrice
                .multiply(BigDecimal.valueOf(0.01))
                .setScale(0, RoundingMode.CEILING);

        return currentPrice.add(increaseAmount);
    }

    private long getBidCount(Auction auction) {
        return 0;
    }

    private boolean canChat(Product product, boolean owner) {
        return !owner && product.getStatus() == ProductStatus.ON_SALE;
    }

    private boolean canBid(Product product, boolean owner) {
        if (owner || product.getSaleType() != ProductSaleType.AUCTION) {
            return false;
        }

        Auction auction = auctionRepository.findByProductProductIdAndDeletedAtIsNullAndProductDeletedAtIsNull(product.getProductId())
                .orElse(null);

        return auction != null && auction.getStatus() == AuctionStatus.AUCTION_LIVE;
    }

    private boolean canPurchase(Product product, boolean owner) {
        return !owner
                && product.getSaleType() == ProductSaleType.REGULAR
                && product.getStatus() == ProductStatus.ON_SALE;
    }

    private boolean canFavorite(Product product, boolean owner) {
        return !owner && product.getStatus() == ProductStatus.ON_SALE;
    }

    private OffsetDateTime toSeoulOffsetDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime.atZone(SEOUL_ZONE).toOffsetDateTime();
    }

    private OffsetDateTime toSeoulOffsetDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime.atZoneSameInstant(SEOUL_ZONE).toOffsetDateTime();
    }
}
