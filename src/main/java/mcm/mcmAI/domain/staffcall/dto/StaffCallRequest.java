package mcm.mcmAI.domain.staffcall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "직원 호출 요청")
public record StaffCallRequest(

        @Schema(description = "제품 ID (모든 호출은 특정 제품과 연결되어야 하므로 필수)", example = "101")
        Long productId,

        @NotBlank
        @Schema(description = "호출 사유", example = "가격 문의")
        String reason
) {
}