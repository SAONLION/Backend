package mcm.mcmAI.domain.pendingaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import mcm.mcmAI.domain.pendingaction.type.ActionNextStep;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "액션 응답 결과")
public record RespondResponse(

        @Schema(description = "액션 ID", example = "1")
        Long actionId,

        @Schema(description = "기록된 응답 값", example = "ask_staff")
        String recordedResponse,

        @Schema(description = "다음 화면 흐름", example = "STAFF_CALL_CREATED")
        String actionNextStep,

        @Schema(description = "결과 상세 (actionNextStep이 STOCK_REQUEST_COMPLETED일 때만 포함)")
        StockCheckResultDTO result
) {

    public static RespondResponse of(
            Long actionId, String responseKey, ActionNextStep nextStep, StockCheckResultDTO result
    ) {
        return new RespondResponse(actionId, responseKey, nextStep.name(), result);
    }
}