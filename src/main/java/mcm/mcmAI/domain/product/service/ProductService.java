package mcm.mcmAI.domain.product.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.pendingaction.entity.PendingActionOption;
import mcm.mcmAI.domain.pendingaction.service.PendingActionService;
import mcm.mcmAI.domain.pendingaction.type.ActionNextStep;
import mcm.mcmAI.domain.pendingaction.type.BlockerType;
import mcm.mcmAI.domain.product.dto.HubOptionDTO;
import mcm.mcmAI.domain.product.dto.HubOptionResponse;
import mcm.mcmAI.domain.product.dto.PickupCheckRequest;
import mcm.mcmAI.domain.product.dto.PickupCheckResponse;
import mcm.mcmAI.domain.product.dto.ProductCreateRequestDTO;
import mcm.mcmAI.domain.product.dto.ProductResponseDTO;
import mcm.mcmAI.domain.product.dto.ProductSummaryDTO;
import mcm.mcmAI.domain.product.dto.ProductTagScanResponseDTO;
import mcm.mcmAI.domain.product.dto.ProductUpdateRequestDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final List<PendingActionOption> CB1_OPTIONS = List.of(
            new PendingActionOption("check_other_store", "타매장 재고 확인", ActionNextStep.STOCK_REQUEST_COMPLETED),
            new PendingActionOption("recommend_alt", "대체 제품 추천하기", ActionNextStep.SHOW_RECOMMENDATIONS)
    );

    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    private final SkuImageRepository skuImageRepository;
    private final TagScanLogService tagScanLogService;
    private final SessionRepository sessionRepository;
    private final PendingActionService pendingActionService;



    @Transactional
    public ProductResponseDTO create(ProductCreateRequestDTO request) {
        Product product = Product.builder()
                .name(request.name())
                .category(request.category())
                .materialDesc(request.materialDesc())
                .heritageDesc(request.heritageDesc())
                .build();

        return ProductResponseDTO.from(productRepository.save(product));
    }

    public ProductResponseDTO getProduct(Long productId) {
        return ProductResponseDTO.from(findProduct(productId));
    }

    public List<ProductResponseDTO> getProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponseDTO::from)
                .toList();
    }

    @Transactional
    public ProductResponseDTO update(Long productId, ProductUpdateRequestDTO request) {
        Product product = findProduct(productId);
        product.update(request.name(), request.category(), request.materialDesc(), request.heritageDesc());
        return ProductResponseDTO.from(product);
    }

    @Transactional
    public void delete(Long productId) {
        productRepository.delete(findProduct(productId));
    }

    @Transactional
    public ProductTagScanResponseDTO getProductByTag(Long tagId, String sessionId) {
        Sku sku = skuRepository.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "태그에 해당하는 SKU를 찾을 수 없습니다: " + tagId));

        System.out.println("=== DEBUG tagId=" + tagId
                + " sku.styleNumber=[" + sku.getStyleNumber() + "]"
                + " sku.styleNumber.length=" + (sku.getStyleNumber() == null ? "null" : sku.getStyleNumber().length()));

        Product product = findProduct(sku.getProduct().getProductId());

        String imageUrl = skuImageRepository
                .findFirstByStyleNumberAndShotTypeOrderByPositionAsc(sku.getStyleNumber(), ShotType.PRODUCT)
                .map(SkuImage::getImageUrl)
                .orElse(null);


        System.out.println("=== DEBUG resolvedImageUrl=" + imageUrl);

        List<HubOptionDTO> hubOptions = HubOptionProvider.firstLevelOptions();

        tagScanLogService.recordScan(sessionId, sku);

        return ProductTagScanResponseDTO.of(ProductSummaryDTO.of(product, imageUrl), hubOptions);
    }

    public List<SubOptionDTO> getHubOptions(Long productId, String interestType) {
        findProduct(productId);
        InterestType type = InterestType.from(interestType);
        return HubOptionProvider.subOptionsOf(type);
    }

    public HubOptionResponse getHubOptionDetail(Long productId, String optionId) {
        findProduct(productId);

        return HubOptionDetailProvider.detailOf(optionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OPTION_NOT_FOUND));
    }

    @Transactional
    public PickupCheckResponse checkPickup(Long productId, String sessionId, PickupCheckRequest request) {
        Product product = findProduct(productId);
        Session session = findSession(sessionId);
        PickupMethod.from(request.pickupMethod());

        Sku sku = skuRepository.findById(request.skuId())
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