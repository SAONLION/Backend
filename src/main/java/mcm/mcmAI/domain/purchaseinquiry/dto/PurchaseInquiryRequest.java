package mcm.mcmAI.domain.purchaseinquiry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "구매 문의 요청")
public record PurchaseInquiryRequest(

        @NotNull
        @Schema(description = "구매 문의를 요청할 SKU", example = "9")
        Long sku
) {
}
