package mcm.mcmAI.domain.pendingaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import mcm.mcmAI.domain.pendingaction.entity.PendingActionOption;

@Schema(description = "팝업 선택 옵션")
public record PendingActionOptionDTO(

        @Schema(description = "옵션 키", example = "ask_staff")
        String key,

        @Schema(description = "옵션 라벨", example = "궁금해요, 직원에게 더 물어보기")
        String label
) {

    public static PendingActionOptionDTO from(PendingActionOption option) {
        return new PendingActionOptionDTO(option.key(), option.label());
    }
}