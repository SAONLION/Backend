package mcm.mcmAI.internal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.staffcall.dto.StaffCallResponse;
import mcm.mcmAI.domain.staffcall.service.StaffCallService;
import mcm.mcmAI.internal.dto.StaffCallTestStatusRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "InternalTest", description = "[시연/테스트 전용] 정식 기능 아님. app.internal-test-endpoints.enabled=true 일 때만 활성화된다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/test/staff-calls")
@ConditionalOnProperty(prefix = "app.internal-test-endpoints", name = "enabled", havingValue = "true")
public class InternalTestStaffCallController {

    private final StaffCallService staffCallService;

    @Operation(
            summary = "[테스트 전용] 직원 호출 상태 강제 변경",
            description = "SA 대시보드가 없는 상태에서 발표 시연을 위해 직원 호출 상태를 수동으로 바꾸는 임시 API. "
                    + "정식 기능이 아니며 인증이 필요 없다. app.internal-test-endpoints.enabled=true(기본값 "
                    + "false)일 때만 이 엔드포인트가 등록되며, 꺼져 있으면 404를 반환한다."
    )
    @PatchMapping("/{callId}/status")
    public StaffCallResponse changeStatus(
            @Parameter(description = "호출 ID", example = "1")
            @PathVariable Long callId,

            @Valid @RequestBody StaffCallTestStatusRequest request
    ) {
        return staffCallService.changeStatusForTest(callId, request.status());
    }
}