package mcm.mcmAI.domain.interactionlog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.interactionlog.dto.InteractionLogRequest;
import mcm.mcmAI.domain.interactionlog.dto.InteractionLogResponse;
import mcm.mcmAI.domain.interactionlog.service.InteractionLogService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "InteractionLog", description = "인터랙션 로그 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sessions/{sessionId}/interactions")
public class InteractionLogController {

    private final InteractionLogService interactionLogService;

    @Operation(
            summary = "인터랙션 기록",
            description = "고객이 제품 상세 화면에서 허브 옵션(1차) 또는 세부 옵션(2차)을 클릭할 때마다 기록을 남긴다. "
                    + "세션이 존재하지 않거나 SKU가 존재하지 않으면 404, interestType이 유효하지 않으면 400을 반환한다."
    )
    @PostMapping
    public InteractionLogResponse recordInteraction(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String sessionId,

            @Valid @RequestBody InteractionLogRequest request
    ) {
        return interactionLogService.record(sessionId, request);
    }
}