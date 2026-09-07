package mcm.mcmAI.domain.staffcall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "직원용 태블릿 보드 (대기/완료 칸)")
public record StaffCallBoardResponse(

        @Schema(description = "대기 중인 호출 목록 (요청 시각 오름차순, 먼저 온 순서)")
        List<StaffCallBoardItem> waiting,

        @Schema(description = "완료된 호출 목록 (최근 완료 순, 최대 completedLimit개)")
        List<StaffCallBoardItem> completed
) {
}
