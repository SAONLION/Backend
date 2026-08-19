package mcm.mcmAI.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.sku.entity.Sku;

@Schema(description = "태그 스캔 시 노출되는 상품 요약 정보")
public record ProductSummaryDTO(

        @Schema(description = "상품 ID", example = "1")
        Long id,

        @Schema(description = "상품명", example = "MCM 백팩")
        String name,

        @Schema(description = "카테고리", example = "BAG")
        String category,

        @Schema(description = "대표 이미지 URL")
        String imageUrl,

        @Schema(description = "상품 설명 (스캔된 SKU 기준, 값이 없으면 null)", nullable = true)
        String description,

        @Schema(description = "짧은 상품 설명 (스캔된 SKU 기준, 값이 없으면 null)", nullable = true)
        String shortDescription
) {

    public static ProductSummaryDTO of(Product product, Sku sku, String imageUrl) {
        return new ProductSummaryDTO(
                product.getProductId(),
                product.getName(),
                product.getCategory(),
                imageUrl,
                sku.getDescription(),
                sku.getShortDescription()
        );
    }
}