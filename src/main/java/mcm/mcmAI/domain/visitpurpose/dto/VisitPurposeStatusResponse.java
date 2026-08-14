package mcm.mcmAI.domain.visitpurpose.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import mcm.mcmAI.domain.visitpurpose.entity.VisitPurpose;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "방문 목적 조회 응답")
public record VisitPurposeStatusResponse(

        @Schema(description = "방문 목적 응답 여부", example = "true")
        boolean answered,

        @Schema(description = "방문 목적 ID", example = "1")
        Long purposeId,

        @Schema(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "방문 목적 타입", example = "TRAVEL")
        String purposeType,

        @Schema(description = "확정 시각")
        LocalDateTime confirmedAt
) {

    public static VisitPurposeStatusResponse notAnswered() {
        return new VisitPurposeStatusResponse(false, null, null, null, null);
    }

    public static VisitPurposeStatusResponse answered(VisitPurpose visitPurpose) {
        return new VisitPurposeStatusResponse(
                true,
                visitPurpose.getPurposeId(),
                visitPurpose.getSession().getSessionId(),
                visitPurpose.getPurposeType().name(),
                visitPurpose.getConfirmedAt()
        );
    }
}
