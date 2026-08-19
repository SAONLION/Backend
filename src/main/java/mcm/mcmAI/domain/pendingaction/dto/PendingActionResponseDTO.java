package mcm.mcmAI.domain.pendingaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import mcm.mcmAI.domain.pendingaction.entity.PendingAction;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "대기 중인 액션(팝업) 조회 응답")
public record PendingActionResponseDTO(

        @Schema(description = "표시할 팝업 존재 여부", example = "true")
        boolean hasAction,

        @Schema(description = "표시할 팝업 상세 (hasAction이 true일 때만 포함)")
        PendingActionDetailDTO action
) {

    public static PendingActionResponseDTO of(PendingAction pendingAction) {
        return new PendingActionResponseDTO(true, PendingActionDetailDTO.from(pendingAction));
    }

    public static PendingActionResponseDTO none() {
        return new PendingActionResponseDTO(false, null);
    }
}