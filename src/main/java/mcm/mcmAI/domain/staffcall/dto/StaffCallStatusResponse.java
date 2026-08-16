package mcm.mcmAI.domain.staffcall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import mcm.mcmAI.domain.staffcall.entity.StaffCall;

@Schema(description = "직원 호출 상태 조회 응답")
public record StaffCallStatusResponse(

        @Schema(description = "호출 ID", example = "1")
        Long callId,

        @Schema(description = "호출 상태", example = "in_progress")
        String status,

        @Schema(description = "상태별 안내 문구", example = "직원이 제품을 준비해서 가는 중이에요")
        String displayMessage,

        @Schema(description = "마지막 상태 변경 시각")
        LocalDateTime updatedAt
) {

    public static StaffCallStatusResponse from(StaffCall staffCall) {
        return new StaffCallStatusResponse(
                staffCall.getCallId(),
                staffCall.getStatus().name().toLowerCase(),
                staffCall.getStatus().getDisplayMessage(),
                staffCall.getUpdatedAt()
        );
    }
}