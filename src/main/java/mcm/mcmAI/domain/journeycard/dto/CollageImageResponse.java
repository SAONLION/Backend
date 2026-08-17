package mcm.mcmAI.domain.journeycard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import mcm.mcmAI.domain.skuimage.type.ShotType;

@Schema(description = "콜라주 구성 이미지")
public record CollageImageResponse(

        @Schema(description = "이미지 URL (S3)", example = "https://cdn.example.com/products/MESCAMM11CO036/01.webp")
        String imageUrl,

        @Schema(description = "샷 타입", example = "MODEL")
        ShotType shotType
) {
}