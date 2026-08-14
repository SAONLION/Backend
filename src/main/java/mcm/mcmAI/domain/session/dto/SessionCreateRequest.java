package mcm.mcmAI.domain.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "세션 생성 요청")
public record SessionCreateRequest(

        @Schema(description = "언어 코드 (ISO 639-1)", example = "ko")
        String language
) {
}