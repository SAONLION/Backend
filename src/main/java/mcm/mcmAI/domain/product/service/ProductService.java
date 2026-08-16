package mcm.mcmAI.domain.product.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.product.dto.HubOptionDTO;
import mcm.mcmAI.domain.product.dto.HubOptionResponse;
import mcm.mcmAI.domain.product.dto.ProductCreateRequestDTO;
import mcm.mcmAI.domain.product.dto.ProductResponseDTO;
import mcm.mcmAI.domain.product.dto.ProductSummaryDTO;
import mcm.mcmAI.domain.product.dto.ProductTagScanResponseDTO;
import mcm.mcmAI.domain.product.dto.ProductUpdateRequestDTO;
import mcm.mcmAI.domain.product.dto.SubOptionDTO;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.entity.ProductImage;
import mcm.mcmAI.domain.product.repository.ProductImageRepository;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.product.type.HubOptionDetailProvider;
import mcm.mcmAI.domain.product.type.HubOptionProvider;
import mcm.mcmAI.domain.product.type.InterestType;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
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

    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    private final ProductImageRepository productImageRepository;
    private final TagScanLogService tagScanLogService;

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

        Product product = findProduct(sku.getProduct().getProductId());

        String imageUrl = productImageRepository.findFirstBySkuOrderBySortOrderAsc(tagId)
                .map(ProductImage::getImageUrl)
                .orElse(null);

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

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}