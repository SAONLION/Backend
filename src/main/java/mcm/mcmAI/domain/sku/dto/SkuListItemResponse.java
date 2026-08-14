package mcm.mcmAI.domain.sku.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import mcm.mcmAI.domain.sku.entity.Sku;

@Schema(description = "색상 탭 전환용 SKU 목록 항목 (가격/재고 미포함)")
public record SkuListItemResponse(

        @Schema(description = "SKU ID", example = "1")
        Long skuId,

        @Schema(description = "색상", example = "Black")
        String color,

        @Schema(description = "대표 이미지 URL")
        String imageUrl
) {

    public static SkuListItemResponse of(Sku sku, String imageUrl) {
        return new SkuListItemResponse(sku.getSku(), sku.getColor(), imageUrl);
    }
}