package mcm.mcmAI.domain.staffcall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "직원 호출 요청")
public record StaffCallRequest(

        @Schema(description = "직전에 스캔/조회한 SKU. 가격·재고·착장·구매 문의 등 제품 문맥이 필요한 호출은 필수이며, "
                + "제품을 아직 태그하지 않은 일반 호출(예: 여권/태그 대기 화면)에서는 생략할 수 있다.", example = "9")
        Long sku,

        @NotBlank
        @Schema(description = "호출 사유", example = "가격 문의")
        String reason
) {
}
