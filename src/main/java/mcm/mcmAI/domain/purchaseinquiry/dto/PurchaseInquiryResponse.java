package mcm.mcmAI.domain.purchaseinquiry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import mcm.mcmAI.domain.purchaseinquiry.entity.PurchaseInquiry;

@Schema(description = "구매 문의 응답")
public record PurchaseInquiryResponse(

        @Schema(description = "구매 문의 ID", example = "1")
        Long purchaseInquiryId,

        @Schema(description = "SKU", example = "9")
        Long sku,

        @Schema(description = "문의 시각")
        LocalDateTime inquiredAt
) {

    public static PurchaseInquiryResponse from(PurchaseInquiry purchaseInquiry) {
        return new PurchaseInquiryResponse(
                purchaseInquiry.getPurchaseInquiryId(),
                purchaseInquiry.getSku().getSku(),
                purchaseInquiry.getInquiredAt()
        );
    }
}
