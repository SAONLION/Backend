package mcm.mcmAI.domain.sku.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import mcm.mcmAI.domain.skuimage.entity.SkuImage;
import mcm.mcmAI.domain.skuimage.type.ShotType;

@Schema(description = "SKU 이미지 정보")
public record SkuImageResponse(

        @Schema(description = "이미지 URL", example = "https://cdn.example.com/sku/1/product-1.jpg")
        String url,

        @Schema(description = "촬영 유형. 제품컷/모델컷 화면 분기의 기준 필드다.", example = "PRODUCT")
        ShotType shotType,

        @Schema(description = "인물이 노출되는 이미지인지 여부(참고용 필드로, 화면 판별 기준은 shotType으로 통일한다)",
                example = "false")
        boolean hasPerson
) {

    public static SkuImageResponse of(SkuImage image) {
        return new SkuImageResponse(image.getImageUrl(), image.getShotType(), image.isHasPerson());
    }
}
