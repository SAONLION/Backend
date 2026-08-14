package mcm.mcmAI.domain.visitpurpose.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.visitpurpose.dto.VisitPurposeRequest;
import mcm.mcmAI.domain.visitpurpose.dto.VisitPurposeResponse;
import mcm.mcmAI.domain.visitpurpose.dto.VisitPurposeStatusResponse;
import mcm.mcmAI.domain.visitpurpose.service.VisitPurposeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "VisitPurpose", description = "방문 목적 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sessions/{sessionId}/visit-purpose")
public class VisitPurposeController {

    private final VisitPurposeService visitPurposeService;

    @Operation(
            summary = "방문 목적 조회",
            description = "세션이 방문 목적을 이미 답변했는지 조회한다. 세션이 존재하지 않으면 404를 반환하고, "
                    + "세션이 존재하면 답변 여부와 무관하게 200을 반환한다 — 답변이 없으면 "
                    + "{ answered: false }, 답변이 있으면 { answered: true, purposeType, ... }."
    )
    @GetMapping
    public VisitPurposeStatusResponse getVisitPurpose(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String sessionId
    ) {
        return visitPurposeService.get(sessionId);
    }

    @Operation(
            summary = "방문 목적 등록",
            description = "세션당 1회만 방문 목적을 저장한다. 이미 답변한 세션이 재요청하면 기존 값을 그대로 "
                    + "반환한다(멱등 처리). confirmedAt은 최초 등록 시각으로 자동 설정된다. 세션이 존재하지 "
                    + "않으면 404, purposeType이 유효하지 않으면 400을 반환한다."
    )
    @PostMapping
    public VisitPurposeResponse saveVisitPurpose(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String sessionId,

            @Valid @RequestBody VisitPurposeRequest request
    ) {
        return visitPurposeService.save(sessionId, request);
    }
}