package mcm.mcmAI.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "[테스트 전용] 착장 요청 요청 시각 강제 변경 요청")
public record TryonRequestTestRequestedAtRequest(

        @NotBlank
        @Schema(description = "착장 요청 소유 세션 ID (소유권 검증용)", example = "abc123")
        String sessionId,

        @NotNull
        @Schema(description = "강제로 설정할 요청 시각",
                example = "2026-08-19T03:00:00")
        LocalDateTime requestedAt
) {
}
