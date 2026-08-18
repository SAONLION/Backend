package mcm.mcmAI.domain.tryonrequest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.tryonrequest.dto.TryonRequestRequest;
import mcm.mcmAI.domain.tryonrequest.dto.TryonRequestResponse;
import mcm.mcmAI.domain.tryonrequest.service.TryonRequestService;
import mcm.mcmAI.global.aop.RequiresActiveSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "TryonRequest", description = "착장 요청 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sessions/{sessionId}/tryon-requests")
public class TryonRequestController {

    private final TryonRequestService tryonRequestService;

    @Operation(
            summary = "착장 요청",
            description = "고객이 사이즈/컬러를 선택하고 직원에게 착장을 요청한다. 직원 도착 여부는 추적하지 않으며 "
                    + "프론트에서 단순 로딩 연출로 처리한다. 세션이 존재하지 않으면 404(SESSION_NOT_FOUND), "
                    + "sku가 존재하지 않으면 404(SKU_NOT_FOUND)를 반환한다."
    )
    @RequiresActiveSession
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TryonRequestResponse createTryonRequest(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String sessionId,

            @Valid @RequestBody TryonRequestRequest request
    ) {
        return tryonRequestService.createTryonRequest(sessionId, request);
    }
}
