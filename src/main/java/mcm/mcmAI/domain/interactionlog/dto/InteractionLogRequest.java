package mcm.mcmAI.domain.interactionlog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "인터랙션 로그 기록 요청")
public record InteractionLogRequest(

        @NotNull
        @Schema(description = "클릭 시점에 보고 있던 SKU", example = "1")
        Long sku,

        @NotBlank
        @Schema(description = "1차 허브 관심사 타입", example = "PRODUCT_UNDERSTANDING")
        String interestType,

        @Schema(description = "2차 세부 옵션 코드 (1차 허브만 클릭한 경우 없음)", example = "MATERIAL")
        String subOption
) {
}