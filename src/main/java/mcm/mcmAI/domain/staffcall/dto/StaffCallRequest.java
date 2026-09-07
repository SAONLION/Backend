package mcm.mcmAI.domain.staffcall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "직원 호출 요청")
public record StaffCallRequest(

        @NotNull
        @Schema(description = "직전에 스캔/조회한 SKU (모든 호출은 특정 SKU와 연결되어야 하므로 필수)", example = "9")
        Long sku,

        @NotBlank
        @Schema(description = "호출 사유", example = "가격 문의")
        String reason
) {
}
