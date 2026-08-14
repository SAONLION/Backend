package mcm.mcmAI.domain.visitpurpose.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import mcm.mcmAI.domain.visitpurpose.entity.VisitPurpose;

@Schema(description = "방문 목적 응답")
public record VisitPurposeResponse(

        @Schema(description = "방문 목적 ID", example = "1")
        Long purposeId,

        @Schema(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "방문 목적 타입", example = "TRAVEL")
        String purposeType,

        @Schema(description = "확정 시각")
        LocalDateTime confirmedAt
) {

    public static VisitPurposeResponse from(VisitPurpose visitPurpose) {
        return new VisitPurposeResponse(
                visitPurpose.getPurposeId(),
                visitPurpose.getSession().getSessionId(),
                visitPurpose.getPurposeType().name(),
                visitPurpose.getConfirmedAt()
        );
    }
}