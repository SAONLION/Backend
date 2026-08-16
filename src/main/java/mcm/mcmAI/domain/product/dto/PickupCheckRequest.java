package mcm.mcmAI.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "수령방법별 재고 확인 요청")
public record PickupCheckRequest(

        @NotBlank
        @Schema(description = "선택한 수령 방법 (호텔로 배송/공항에서 수령/귀국지로 배송/매장에서 바로 수령 중 하나)",
                example = "매장에서 바로 수령")
        String pickupMethod,

        @NotNull
        @Schema(description = "확인할 SKU ID", example = "9")
        Long skuId
) {
}