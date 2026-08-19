package mcm.mcmAI.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "[테스트 전용] 직원 호출 요청 시각 강제 변경 요청")
public record StaffCallTestRequestedAtRequest(

        @NotBlank
        @Schema(description = "호출 소유 세션 ID (소유권 검증용)", example = "abc123")
        String sessionId,

        @NotNull
        @Schema(description = "강제로 설정할 요청 시각 (과거 시각을 지정해 CB3 5분 타임아웃을 재현할 수 있다)",
                example = "2026-08-18T10:00:00")
        LocalDateTime requestedAt
) {
}
