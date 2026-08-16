package mcm.mcmAI.domain.staffcall.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.staffcall.dto.StaffCallRequest;
import mcm.mcmAI.domain.staffcall.dto.StaffCallResponse;
import mcm.mcmAI.domain.staffcall.dto.StaffCallStatusResponse;
import mcm.mcmAI.domain.staffcall.service.StaffCallService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "StaffCall", description = "직원 호출 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/session/staff-calls")
public class StaffCallController {

    private final StaffCallService staffCallService;

    @Operation(
            summary = "직원 호출 요청",
            description = "고객이 직원 호출을 요청한다. 고객 측 트리거만 담당하며 SA 응대 화면은 범위 밖이다. "
                    + "모든 호출은 특정 제품과 연결되어야 하므로 productId 없이는 요청할 수 없다. 세션이 존재하지 "
                    + "않으면 404(SESSION_NOT_FOUND), productId가 존재하지 않는 제품이면 404(PRODUCT_NOT_FOUND), "
                    + "productId가 비어있으면 400(MISSING_PRODUCT_ID)을 반환한다."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffCallResponse createStaffCall(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String sessionId,

            @Valid @RequestBody StaffCallRequest request
    ) {
        return staffCallService.createStaffCall(sessionId, request);
    }

    @Operation(
            summary = "직원 호출 상태 조회",
            description = "3~5초 간격으로 폴링해 직원 호출 상태를 확인한다. status는 requested → acknowledged → "
                    + "in_progress → completed 순으로 전이되며, displayMessage에 상태별 한국어 안내 문구가 함께 "
                    + "내려간다. callId가 해당 세션의 호출이 아니면 404(CALL_NOT_FOUND)를 반환한다."
    )
    @GetMapping("/{callId}")
    public StaffCallStatusResponse getStaffCall(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String sessionId,

            @Parameter(description = "호출 ID", example = "1")
            @PathVariable Long callId
    ) {
        return staffCallService.getStaffCall(sessionId, callId);
    }
}