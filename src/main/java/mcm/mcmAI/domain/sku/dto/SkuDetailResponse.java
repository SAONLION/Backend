package mcm.mcmAI.domain.sku.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import mcm.mcmAI.domain.sku.entity.Sku;

@Schema(description = "SKU 상세 정보 (가격/재고 미포함)")
public record SkuDetailResponse(

        @Schema(description = "SKU ID", example = "1")
        Long skuId,

        @Schema(description = "색상", example = "Black")
        String color,

        @Schema(description = "사이즈(콤마로 구분된 선택 가능 사이즈 목록)", example = "XS,S,M,L,XL")
        String size,

        @Schema(description = "이미지 URL 목록 (정렬 순서대로)")
        List<String> images
) {

    public static SkuDetailResponse of(Sku sku, List<String> images) {
        return new SkuDetailResponse(sku.getSku(), sku.getColor(), sku.getSize(), images);
    }
}