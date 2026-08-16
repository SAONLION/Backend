package mcm.mcmAI.domain.pendingaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "액션 응답 요청")
public record RespondRequest(

        @NotBlank
        @Schema(description = "선택한 옵션 키. 옵션 선택 없이 팝업을 닫은 경우 예약값 'dismissed'", example = "ask_staff")
        String responseKey
) {
}