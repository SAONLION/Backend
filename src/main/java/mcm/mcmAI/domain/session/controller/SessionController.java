package mcm.mcmAI.domain.session.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.session.dto.NicknameUpdateRequest;
import mcm.mcmAI.domain.session.dto.NicknameUpdateResponse;
import mcm.mcmAI.domain.session.dto.SessionCreateRequest;
import mcm.mcmAI.domain.session.dto.SessionCreateResponse;
import mcm.mcmAI.domain.session.dto.SessionEndResponse;
import mcm.mcmAI.domain.session.service.SessionService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Session", description = "익명 세션 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService sessionService;

    @Operation(
            summary = "세션 생성",
            description = "새로운 익명 세션을 발급한다. session_id는 UUID로 생성되며 인증 토큰은 발급하지 않는다."
    )
    @PostMapping
    public SessionCreateResponse createSession(
            @RequestBody(required = false) SessionCreateRequest request
    ) {
        return sessionService.create(request);
    }

    @Operation(
            summary = "닉네임 변경",
            description = "SA 대시보드에서 고객-세션을 매칭하기 위한 닉네임을 저장한다. 1~20자여야 하며, "
                    + "세션이 존재하지 않으면 404를 반환한다."
    )
    @PatchMapping("/{sessionId}/nickname")
    public NicknameUpdateResponse updateNickname(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String sessionId,

            @Valid @RequestBody NicknameUpdateRequest request
    ) {
        return sessionService.updateNickname(sessionId, request);
    }

    @Operation(
            summary = "세션 종료",
            description = "세션을 종료 처리한다(ended_at 설정). 이미 종료된 세션에 다시 요청해도 동일한 결과를 "
                    + "멱등적으로 반환한다. 세션이 존재하지 않으면 404를 반환한다."
    )
    @PostMapping("/{sessionId}/end")
    public SessionEndResponse endSession(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String sessionId
    ) {
        return sessionService.end(sessionId);
    }
}