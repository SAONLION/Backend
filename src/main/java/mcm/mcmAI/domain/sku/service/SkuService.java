package mcm.mcmAI.domain.sku.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.product.entity.ProductImage;
import mcm.mcmAI.domain.product.repository.ProductImageRepository;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.sku.dto.SkuDetailResponse;
import mcm.mcmAI.domain.sku.dto.SkuListItemResponse;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkuService {

    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    public List<SkuListItemResponse> getSkus(Long productId) {
        ensureProductExists(productId);

        return skuRepository.findByProduct_ProductIdOrderBySkuAsc(productId).stream()
                .map(sku -> SkuListItemResponse.of(sku, representativeImageUrl(sku.getSku())))
                .toList();
    }

    public SkuDetailResponse getSkuDetail(Long productId, Long skuId) {
        ensureProductExists(productId);

        Sku sku = skuRepository.findById(skuId)
                .filter(found -> found.getProduct().getProductId().equals(productId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "SKU를 찾을 수 없습니다: " + skuId));

        List<String> images = productImageRepository.findBySkuOrderBySortOrderAsc(skuId).stream()
                .map(ProductImage::getImageUrl)
                .toList();

        return SkuDetailResponse.of(sku, images);
    }

    private String representativeImageUrl(Long skuId) {
        return productImageRepository.findFirstBySkuOrderBySortOrderAsc(skuId)
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }

    private void ensureProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "제품을 찾을 수 없습니다: " + productId);
        }
    }
}