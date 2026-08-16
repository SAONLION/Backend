package mcm.mcmAI.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import mcm.mcmAI.domain.product.type.HubOptionType;
import mcm.mcmAI.domain.product.type.ProductFlowNextStep;

@Schema(description = "허브 하위 옵션 상세 응답")
public record HubOptionResponse(

        @Schema(description = "하위 옵션 ID", example = "9")
        String optionId,

        @Schema(description = "옵션 타입 (INFO: 바로 내용 노출, STAFF_MEDIATED: 다음 단계 안내)", example = "INFO")
        String type,

        @Schema(description = "제목", example = "수령 방법 안내")
        String title,

        @Schema(description = "내용", example = "호텔 배송, 공항 수령, 귀국지 배송, 매장 수령 중 선택하실 수 있어요.")
        String content,

        @Schema(description = "다음 단계", example = "NONE")
        String nextStep,

        @Schema(description = "수령 방법 목록 (nextStep이 SELECT_PICKUP_METHOD일 때만 제공)", nullable = true)
        List<String> pickupMethods
) {

    public static HubOptionResponse info(String optionId, String title, String content) {
        return new HubOptionResponse(
                optionId, HubOptionType.INFO.name(), title, content, ProductFlowNextStep.NONE.name(), null
        );
    }

    public static HubOptionResponse staffMediated(
            String optionId, String title, String content,
            ProductFlowNextStep nextStep, List<String> pickupMethods
    ) {
        return new HubOptionResponse(
                optionId, HubOptionType.STAFF_MEDIATED.name(), title, content, nextStep.name(), pickupMethods
        );
    }
}