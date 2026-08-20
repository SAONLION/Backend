package mcm.mcmAI.domain.product.service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.pendingaction.entity.PendingActionOption;
import mcm.mcmAI.domain.pendingaction.service.PendingActionService;
import mcm.mcmAI.domain.pendingaction.type.ActionNextStep;
import mcm.mcmAI.domain.pendingaction.type.BlockerType;
import mcm.mcmAI.domain.product.dto.HubOptionDTO;
import mcm.mcmAI.domain.product.dto.HubOptionResponse;
import mcm.mcmAI.domain.product.dto.PickupCheckRequest;
import mcm.mcmAI.domain.product.dto.PickupCheckResponse;
import mcm.mcmAI.domain.product.dto.ProductSummaryDTO;
import mcm.mcmAI.domain.product.dto.ProductTagScanResponseDTO;
import mcm.mcmAI.domain.product.dto.RandomTagResponse;
import mcm.mcmAI.domain.product.dto.SubOptionDTO;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.product.type.HubOptionDetailProvider;
import mcm.mcmAI.domain.product.type.HubOptionProvider;
import mcm.mcmAI.domain.product.type.InterestType;
import mcm.mcmAI.domain.product.type.PickupMethod;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import mcm.mcmAI.domain.skuimage.entity.SkuImage;
import mcm.mcmAI.domain.skuimage.repository.SkuImageRepository;
import mcm.mcmAI.domain.skuimage.type.ShotType;
import mcm.mcmAI.domain.tagscanlog.service.TagScanLogService;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final List<PendingActionOption> CB1_OPTIONS = List.of(
            new PendingActionOption("check_other_store", "타매장 재고 확인", ActionNextStep.STOCK_REQUEST_COMPLETED),
            new PendingActionOption("recommend_alt", "대체 제품 추천하기", ActionNextStep.SHOW_RECOMMENDATIONS)
    );

    // optionId 7(다른 제품과 비교), 8(스타일링 추천)은 카탈로그에 매핑 가능한 원본 데이터가 없어
    // HubOptionDetailProvider의 기존 고정 안내문을 그대로 사용한다(P2-4 스킵 대상).
    private static final Set<String> PRODUCT_LEVEL_OPTION_IDS = Set.of("1", "2", "3", "4", "5", "6");

    // 소재/헤리티지/관리(안감)/원산지는 색상(SKU)마다 원본 값이 다를 수 있다(예: Ottomar 위켄더
    // 핑크=16K, 코냑=24K 하드웨어). skuId가 주어지면 이 옵션들만 해당 SKU 하나로 좁혀서 조회한다.
    // 사이즈 가이드(5)·컬러 옵션(6)은 상품 전체 SKU를 모아 보여주는 것이 의도이므로 대상에서 제외한다.
    private static final Set<String> SKU_SCOPED_OPTION_IDS = Set.of("1", "2", "3", "4");

    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    private final SkuImageRepository skuImageRepository;
    private final TagScanLogService tagScanLogService;
    private final SessionRepository sessionRepository;
    private final PendingActionService pendingActionService;


    @Transactional
    public ProductTagScanResponseDTO getProductByTag(Long tagId, String sessionId) {
        Sku sku = skuRepository.findBySkuAndIsDeletedFalse(tagId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKU_NOT_FOUND));

        Product product = findProduct(sku.getProduct().getProductId());

        String imageUrl = skuImageRepository
                .findFirstByStyleNumberAndShotTypeAndIsDeletedFalseOrderByPositionAsc(sku.getStyleNumber(), ShotType.PRODUCT)
                .map(SkuImage::getImageUrl)
                .orElse(null);

        List<HubOptionDTO> hubOptions = HubOptionProvider.firstLevelOptions();

        tagScanLogService.recordScan(sessionId, sku);

        return ProductTagScanResponseDTO.of(ProductSummaryDTO.of(product, sku, imageUrl), hubOptions);
    }

    // 태그 스캔 로그를 남기지 않는 단순 무작위 선택. 프론트가 반환된 tagId로 scanTag를 호출해야 실제 스캔 기록이 남는다.
    public RandomTagResponse getRandomTag(String category) {
        String normalizedCategory = (category == null || category.isBlank()) ? null : category;
        Long tagId = skuRepository.findRandomActiveByCategory(normalizedCategory)
                .map(Sku::getSku)
                .orElse(null);
        return new RandomTagResponse(tagId);
    }

    public List<SubOptionDTO> getHubOptions(Long productId, String interestType) {
        findProduct(productId);
        InterestType type = InterestType.from(interestType);
        return HubOptionProvider.subOptionsOf(type);
    }

    public HubOptionResponse getHubOptionDetail(Long productId, String optionId, Long skuId) {
        findProduct(productId);

        return buildProductLevelDetail(productId, optionId, skuId)
                .or(() -> HubOptionDetailProvider.detailOf(optionId))
                .orElseThrow(() -> new BusinessException(ErrorCode.OPTION_NOT_FOUND));
    }

    // optionId 1~6은 productId(+선택적으로 skuId)로 호출된다. skuId가 주어지고 색상별로 값이
    // 달라질 수 있는 옵션(1~4)이면 해당 SKU 하나의 값만 사용한다. skuId가 없으면(레거시 호출) 상품의
    // SKU 목록 중 각 필드가 채워진 첫 번째 값을 대표값으로 사용한다. sku.color/size처럼 SKU마다
    // 달라지는 값(5, 6)은 skuId와 무관하게 항상 전체 SKU에서 값을 모아 보여준다.
    private Optional<HubOptionResponse> buildProductLevelDetail(Long productId, String optionId, Long skuId) {
        if (!PRODUCT_LEVEL_OPTION_IDS.contains(optionId)) {
            return Optional.empty();
        }

        List<Sku> skus = skuRepository.findByProduct_ProductIdAndIsDeletedFalseOrderBySkuAsc(productId);
        List<Sku> targetSkus = resolveTargetSkus(skus, optionId, skuId);
        String content = switch (optionId) {
            case "1" -> materialContent(targetSkus);
            case "2" -> heritageContent(targetSkus);
            case "3" -> firstNonBlank(targetSkus, Sku::getLiningCareText);
            case "4" -> firstNonBlank(targetSkus, Sku::getCountryOfOrigin);
            case "5" -> sizeGuideContent(skus);
            case "6" -> colorOptionContent(skus);
            default -> null;
        };

        return HubOptionDetailProvider.detailOf(optionId)
                .map(base -> new HubOptionResponse(
                        base.optionId(), base.type(), base.title(), content, base.nextStep(), base.pickupMethods()));
    }

    private List<Sku> resolveTargetSkus(List<Sku> skus, String optionId, Long skuId) {
        if (skuId == null || !SKU_SCOPED_OPTION_IDS.contains(optionId)) {
            return skus;
        }
        return skus.stream()
                .filter(sku -> sku.getSku().equals(skuId))
                .findFirst()
                .map(List::of)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKU_NOT_FOUND));
    }

    private String materialContent(List<Sku> skus) {
        String body = firstNonBlank(skus, Sku::getBodyMaterial);
        String trim = firstNonBlank(skus, Sku::getTrimMaterial);
        String hardware = firstNonBlank(skus, Sku::getHardwareText);
        String lining = firstNonBlank(skus, Sku::getLiningCareText);
        String countryOfOrigin = firstNonBlank(skus, Sku::getCountryOfOrigin);
        String sustainability = firstNonBlank(skus, Sku::getSustainabilityCertification);
        if (body == null && trim == null && hardware == null && lining == null
                && countryOfOrigin == null && sustainability == null) {
            return null;
        }
        StringBuilder content = new StringBuilder();
        if (body != null) {
            content.append("바디: ").append(body);
        }
        if (trim != null) {
            appendLine(content, "트림: " + trim);
        }
        if (hardware != null) {
            appendLine(content, hardware);
        }
        if (lining != null) {
            appendLine(content, lining);
        }
        if (countryOfOrigin != null) {
            appendLine(content, "제조국: " + countryOfOrigin);
        }
        if (sustainability != null) {
            appendLine(content, "지속가능성 인증: " + sustainability);
        }
        return content.toString();
    }

    private void appendLine(StringBuilder content, String line) {
        if (content.length() > 0) {
            content.append('\n');
        }
        content.append(line);
    }

    private String heritageContent(List<Sku> skus) {
        String shortDescription = firstNonBlank(skus, Sku::getShortDescription);
        String description = firstNonBlank(skus, Sku::getDescription);
        if (shortDescription == null && description == null) {
            return null;
        }
        StringBuilder content = new StringBuilder();
        if (shortDescription != null) {
            content.append(shortDescription);
        }
        if (description != null) {
            if (content.length() > 0) {
                content.append("\n\n");
            }
            content.append(description);
        }
        return content.toString();
    }

    private String sizeGuideContent(List<Sku> skus) {
        List<String> sizes = skus.stream()
                .map(Sku::getSize)
                .filter(Objects::nonNull)
                .flatMap(size -> Arrays.stream(size.split(",")))
                .map(String::trim)
                .filter(size -> !size.isBlank())
                .distinct()
                .toList();
        return sizes.isEmpty() ? null : String.join(", ", sizes);
    }

    private String colorOptionContent(List<Sku> skus) {
        List<String> colors = skus.stream()
                .map(Sku::getColor)
                .filter(color -> color != null && !color.isBlank())
                .distinct()
                .toList();
        return colors.isEmpty() ? null : String.join(", ", colors);
    }

    private String firstNonBlank(List<Sku> skus, Function<Sku, String> extractor) {
        return skus.stream()
                .map(extractor)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public PickupCheckResponse checkPickup(Long productId, String sessionId, PickupCheckRequest request) {
        Product product = findProduct(productId);
        Session session = findSession(sessionId);
        PickupMethod.from(request.pickupMethod());

        Sku sku = skuRepository.findBySkuAndIsDeletedFalse(request.skuId())
                .filter(found -> found.getProduct().getProductId().equals(productId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SKU_NOT_FOUND));

        if (hasStock(sku)) {
            return PickupCheckResponse.inStock();
        }

        pendingActionService.createBlocker(
                session, BlockerType.CB1, product,
                popupTitle(sku), popupBody(product), CB1_OPTIONS
        );

        return PickupCheckResponse.blockerTriggered();
    }

    private boolean hasStock(Sku sku) {
        Integer stockQty = sku.getStockQty();
        return stockQty != null && stockQty > 0;
    }

    private String popupTitle(Sku sku) {
        return "찾으시는 %s / %s는 현재 이 매장에 재고가 없습니다".formatted(sku.getColor(), sku.getSize());
    }

    private String popupBody(Product product) {
        return "%s의 재고가 준비되는 대로 안내해드릴게요. 다른 매장 재고를 확인하거나 대체 상품을 추천받아보세요."
                .formatted(product.getName());
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private Session findSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }
}