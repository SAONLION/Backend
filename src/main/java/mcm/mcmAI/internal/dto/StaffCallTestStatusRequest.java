package mcm.mcmAI.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "[테스트 전용] 직원 호출 상태 변경 요청")
public record StaffCallTestStatusRequest(

        @NotBlank
        @Schema(description = "변경할 상태 (requested/acknowledged/in_progress/completed)", example = "in_progress")
        String status
) {
}