package mcm.mcmAI.domain.pendingaction.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.pendingaction.dto.RespondRequestDTO;
import mcm.mcmAI.domain.pendingaction.dto.RespondResponseDTO;
import mcm.mcmAI.domain.pendingaction.service.PendingActionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "PendingAction", description = "대기 중인 Blocker 팝업 조회/응답 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/actions")
public class ActionController {

    private final PendingActionService pendingActionService;

    @Operation(
            summary = "Blocker 팝업 응답 기록",
            description = "고객이 Blocker 팝업(pending_action)에서 선택한 응답을 기록하고 다음 화면 흐름"
                    + "(actionNextStep)을 반환한다. responseKey는 해당 액션의 옵션 목록(options[].key) 중 하나이거나 "
                    + "예약값 'dismissed'여야 하며, 'dismissed'는 옵션 선택 없이 팝업을 닫은 경우로 항상 유효하게 "
                    + "처리되고 actionNextStep=NONE을 반환한다. actionNextStep이 STOCK_REQUEST_COMPLETED인 경우 "
                    + "(CB1의 타매장 재고 확인) result 필드에 더미 재고 확인 결과가 함께 내려간다. 이미 응답이 기록된 "
                    + "액션(status=RESPONDED)에 재요청해도 에러 없이 최신 응답으로 덮어쓴다. actionId가 존재하지 "
                    + "않으면 404(ACTION_NOT_FOUND), responseKey가 유효하지 않으면 400(INVALID_RESPONSE_KEY)을 "
                    + "반환한다."
    )
    @PostMapping("/{actionId}/respond")
    public RespondResponseDTO respond(
            @Parameter(description = "액션 ID", example = "1")
            @PathVariable Long actionId,

            @Valid @RequestBody RespondRequestDTO request
    ) {
        return pendingActionService.respond(actionId, request);
    }
}