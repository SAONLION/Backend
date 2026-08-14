package mcm.mcmAI.domain.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import mcm.mcmAI.domain.session.entity.Session;

@Schema(description = "세션 종료 응답")
public record SessionEndResponse(

        @Schema(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "세션 상태", example = "ended")
        String status,

        @Schema(description = "종료 시각")
        LocalDateTime endedAt
) {

    public static SessionEndResponse from(Session session) {
        return new SessionEndResponse(session.getSessionId(), "ended", session.getEndedAt());
    }
}