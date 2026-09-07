package mcm.mcmAI.domain.staffcall.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.staffcall.dto.StaffCallBoardResponse;
import mcm.mcmAI.domain.staffcall.dto.StaffCallStatusResponse;
import mcm.mcmAI.domain.staffcall.service.StaffCallService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "StaffCallBoard", description = "직원용 태블릿 호출 보드 API (세션 무관, 매장 전체 호출 대상). "
        + "모든 요청에 X-Staff-Token 헤더가 필요하며, 값이 app.staff-board.token과 일치하지 않으면 "
        + "401(STAFF_TOKEN_INVALID)을 반환한다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff/staff-calls")
public class StaffCallBoardController {

    private static final int DEFAULT_COMPLETED_LIMIT = 20;

    private final StaffCallService staffCallService;

    @Operation(
            summary = "직원 호출 보드 조회",
            description = "직원용 태블릿이 3~5초 간격으로 폴링하는 대기/완료 칸 목록. 대기 칸은 상태가 completed가 "
                    + "아닌 모든 호출을 요청 시각 오름차순(먼저 온 순서)으로, 완료 칸은 completedLimit개까지만 "
                    + "최근 완료 순으로 내려간다(기본 20개, 무제한 누적 방지)."
    )
    @GetMapping
    public StaffCallBoardResponse getBoard(
            @Parameter(description = "완료 칸에 표시할 최대 개수", example = "20")
            @RequestParam(defaultValue = "" + DEFAULT_COMPLETED_LIMIT) int completedLimit
    ) {
        return staffCallService.getBoard(completedLimit);
    }

    @Operation(
            summary = "직원 호출 완료 처리",
            description = "직원이 태블릿에서 체크 버튼을 눌러 호출을 완료 칸으로 옮긴다. status를 바로 completed로 "
                    + "전이시킨다. callId가 존재하지 않으면 404(CALL_NOT_FOUND)를 반환한다."
    )
    @PatchMapping("/{callId}/complete")
    public StaffCallStatusResponse completeStaffCall(
            @Parameter(description = "호출 ID", example = "1")
            @PathVariable Long callId
    ) {
        return staffCallService.completeStaffCall(callId);
    }
}
