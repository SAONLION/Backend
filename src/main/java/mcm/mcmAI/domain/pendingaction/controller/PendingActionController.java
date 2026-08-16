package mcm.mcmAI.domain.pendingaction.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.pendingaction.dto.PendingActionResponse;
import mcm.mcmAI.domain.pendingaction.service.PendingActionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "PendingAction", description = "대기 중인 Blocker 팝업 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/session/pending-action")
public class PendingActionController {

    private final PendingActionService pendingActionService;

    @Operation(
            summary = "대기 중인 팝업(Blocker 흐름 1단계) 조회",
            description = "서버가 내부 규칙으로 Blocker(CB1/CB3/CB5/CB6)를 감지하면 세션에 PENDING 상태 "
                    + "액션이 생성된다. 클라이언트는 이 API를 3~5초 간격으로 폴링해 지금 띄울 팝업(흐름의 1단계)이 "
                    + "있는지 확인한다. 세션의 PENDING 상태 액션 중 가장 최근 1건을 반환하며, 없으면 "
                    + "hasAction=false를 반환한다. 이후 단계(이메일 폼 등) 흐름 제어는 이 API가 아니라 "
                    + "POST /actions/{actionId}/respond의 actionNextStep이 담당한다. 세션이 존재하지 않으면 "
                    + "404를 반환한다."
    )
    @GetMapping
    public PendingActionResponse getPendingAction(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String sessionId
    ) {
        return pendingActionService.getPendingAction(sessionId);
    }
}