package mcm.mcmAI.domain.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import mcm.mcmAI.domain.session.entity.Session;

@Schema(description = "닉네임 변경 응답")
public record NicknameUpdateResponse(

        @Schema(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "닉네임", example = "손님1")
        String nickname
) {

    public static NicknameUpdateResponse from(Session session) {
        return new NicknameUpdateResponse(session.getSessionId(), session.getNickname());
    }
}