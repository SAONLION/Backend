package mcm.mcmAI.domain.interactionlog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import mcm.mcmAI.domain.interactionlog.entity.InteractionLog;

@Schema(description = "인터랙션 로그 기록 응답")
public record InteractionLogResponse(

        @Schema(description = "인터랙션 ID", example = "1")
        Long interactionId,

        @Schema(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "SKU", example = "1")
        Long sku,

        @Schema(description = "1차 허브 관심사 타입", example = "PRODUCT_UNDERSTANDING")
        String interestType,

        @Schema(description = "2차 세부 옵션 코드", example = "MATERIAL")
        String subOption
) {

    public static InteractionLogResponse from(InteractionLog interactionLog) {
        return new InteractionLogResponse(
                interactionLog.getInteractionId(),
                interactionLog.getSession().getSessionId(),
                interactionLog.getSku().getSku(),
                interactionLog.getInterestType().name(),
                interactionLog.getSubOption()
        );
    }
}