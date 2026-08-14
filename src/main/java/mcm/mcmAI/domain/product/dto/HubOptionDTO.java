package mcm.mcmAI.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import mcm.mcmAI.domain.product.type.InterestType;

@Schema(description = "1차 허브 옵션")
public record HubOptionDTO(

        @Schema(description = "관심사 타입", example = "PRODUCT_UNDERSTANDING")
        String type,

        @Schema(description = "화면에 노출되는 라벨", example = "상품 이해")
        String label
) {

    public static HubOptionDTO from(InterestType interestType) {
        return new HubOptionDTO(interestType.name(), interestType.getLabel());
    }
}