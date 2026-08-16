package mcm.mcmAI.domain.staffcall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import mcm.mcmAI.domain.staffcall.entity.StaffCall;

@Schema(description = "직원 호출 생성/상태 변경 응답")
public record StaffCallResponse(

        @Schema(description = "호출 ID", example = "1")
        Long callId,

        @Schema(description = "호출 상태", example = "requested")
        String status,

        @Schema(description = "요청 시각")
        LocalDateTime requestedAt
) {

    public static StaffCallResponse from(StaffCall staffCall) {
        return new StaffCallResponse(
                staffCall.getCallId(),
                staffCall.getStatus().name().toLowerCase(),
                staffCall.getRequestedAt()
        );
    }
}