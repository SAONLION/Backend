package mcm.mcmAI.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import mcm.mcmAI.domain.product.type.ProductFlowNextStep;

@Schema(description = "수령방법별 재고 확인 응답")
public record PickupCheckResponse(

        @Schema(description = "재고 보유 여부", example = "true")
        boolean available,

        @Schema(description = "다음 단계 (재고 없으면 BLOCKER_TRIGGERED와 함께 CB1 팝업이 자동 생성된다)", example = "NONE")
        String nextStep,

        @Schema(description = "안내 메시지", example = "재고가 확인되었습니다.")
        String message
) {

    public static PickupCheckResponse inStock() {
        return new PickupCheckResponse(true, ProductFlowNextStep.NONE.name(), "재고가 확인되었습니다.");
    }

    public static PickupCheckResponse blockerTriggered() {
        return new PickupCheckResponse(
                false, ProductFlowNextStep.BLOCKER_TRIGGERED.name(), "선택하신 옵션의 재고가 없습니다.");
    }
}