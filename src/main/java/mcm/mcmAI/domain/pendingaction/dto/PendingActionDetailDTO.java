package mcm.mcmAI.domain.pendingaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import mcm.mcmAI.domain.pendingaction.entity.PendingAction;

@Schema(description = "대기 중인 액션(팝업) 상세")
public record PendingActionDetailDTO(

        @Schema(description = "액션 ID", example = "1")
        Long actionId,

        @Schema(description = "블로커 유형 (프론트 표시용, F23-1 통합 계약)", example = "CONTENT_OFFER")
        String blockerType,

        @Schema(description = "블로커 원문 그룹 (분석용, CB5/CB6 원문 그대로)", example = "CB5")
        String ruleGroup,

        @Schema(description = "트리거 ID", example = "T-CB6-a")
        String triggerId,

        @Schema(description = "신호 등급 (1=확정, 2=추정)", example = "2")
        Integer tier,

        @Schema(description = "관련 제품 ID (제품과 무관한 블로커는 null)", example = "1", nullable = true)
        Long productId,

        @Schema(description = "팝업 제목", example = "관심있어 하시던 백팩")
        String popupTitle,

        @Schema(description = "팝업 본문", example = "비 오는 날에도 젖지 않는다는 것, 알고 계셨나요?")
        String popupBody,

        @Schema(description = "선택 옵션 목록")
        List<PendingActionOptionDTO> options
) {

    public static PendingActionDetailDTO from(PendingAction pendingAction) {
        Long productId = pendingAction.getProduct() == null
                ? null
                : pendingAction.getProduct().getProductId();

        return new PendingActionDetailDTO(
                pendingAction.getActionId(),
                pendingAction.getBlockerType().toDisplayType(),
                pendingAction.getBlockerType().name(),
                pendingAction.getTriggerId(),
                pendingAction.getTier(),
                productId,
                pendingAction.getPopupTitle(),
                pendingAction.getPopupBody(),
                pendingAction.getOptions().stream()
                        .map(PendingActionOptionDTO::from)
                        .toList()
        );
    }
}