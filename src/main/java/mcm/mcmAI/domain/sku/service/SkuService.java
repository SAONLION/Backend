package mcm.mcmAI.domain.sku.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.sku.dto.SkuDetailResponse;
import mcm.mcmAI.domain.sku.dto.SkuListItemResponse;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import mcm.mcmAI.domain.skuimage.entity.SkuImage;
import mcm.mcmAI.domain.skuimage.repository.SkuImageRepository;
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
    private final SkuImageRepository skuImageRepository;

    public List<SkuListItemResponse> getSkus(Long productId) {
        ensureProductExists(productId);

        List<Sku> skus = skuRepository.findByProduct_ProductIdOrderBySkuAsc(productId);

        List<String> styleNumbers = skus.stream()
                .map(Sku::getStyleNumber)
                .filter(styleNumber -> styleNumber != null)
                .toList();

        Map<String, List<SkuImage>> imagesByStyle = skuImageRepository.findByStyleNumberIn(styleNumbers).stream()
                .collect(Collectors.groupingBy(SkuImage::getStyleNumber));

        return skus.stream()
                .map(sku -> SkuListItemResponse.of(
                        sku, representativeImageUrl(imagesByStyle.get(sku.getStyleNumber()))))
                .toList();
    }

    public SkuDetailResponse getSkuDetail(Long productId, Long skuId) {
        ensureProductExists(productId);

        Sku sku = skuRepository.findById(skuId)
                .filter(found -> found.getProduct().getProductId().equals(productId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "SKU를 찾을 수 없습니다: " + skuId));

        List<String> images = sku.getStyleNumber() == null
                ? List.of()
                : skuImageRepository.findByStyleNumberOrderByPositionAsc(sku.getStyleNumber()).stream()
                        .map(SkuImage::getImageUrl)
                        .toList();

        return SkuDetailResponse.of(sku, images);
    }

    private String representativeImageUrl(List<SkuImage> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .min(Comparator.comparing(SkuImage::getPosition))
                .map(SkuImage::getImageUrl)
                .orElse(null);
    }

    private void ensureProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "제품을 찾을 수 없습니다: " + productId);
        }
    }
}
