package mcm.mcmAI.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import mcm.mcmAI.domain.product.entity.Product;

@Schema(description = "태그 스캔 시 노출되는 상품 요약 정보")
public record ProductSummaryDTO(

        @Schema(description = "상품 ID", example = "1")
        Long id,

        @Schema(description = "상품명", example = "MCM 백팩")
        String name,

        @Schema(description = "카테고리", example = "BAG")
        String category,

        @Schema(description = "대표 이미지 URL")
        String imageUrl
) {

    public static ProductSummaryDTO of(Product product, String imageUrl) {
        return new ProductSummaryDTO(
                product.getProductId(),
                product.getName(),
                product.getCategory(),
                imageUrl
        );
    }
}