package mcm.mcmAI.domain.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import mcm.mcmAI.domain.session.entity.Session;

@Schema(description = "세션 생성 응답")
public record SessionCreateResponse(

        @Schema(description = "세션 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "언어 코드", example = "ko")
        String language,

        @Schema(description = "생성 시각")
        LocalDateTime createdAt
) {

    public static SessionCreateResponse from(Session session) {
        return new SessionCreateResponse(session.getSessionId(), session.getLanguage(), session.getCreatedAt());
    }
}