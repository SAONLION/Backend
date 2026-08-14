package mcm.mcmAI.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "태그 스캔 응답")
public record ProductTagScanResponseDTO(

        ProductSummaryDTO product,

        @Schema(description = "1차 허브 옵션 (4종 고정)")
        List<HubOptionDTO> hubOptions
) {

    public static ProductTagScanResponseDTO of(ProductSummaryDTO product, List<HubOptionDTO> hubOptions) {
        return new ProductTagScanResponseDTO(product, hubOptions);
    }
}