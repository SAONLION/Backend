package mcm.mcmAI.domain.staffcall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.staffcall.entity.StaffCall;

@Schema(description = "직원용 태블릿 보드에 표시되는 호출 1건")
public record StaffCallBoardItem(

        @Schema(description = "호출 ID", example = "1")
        Long callId,

        @Schema(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "고객 닉네임", example = "손님1")
        String nickname,

        @Schema(description = "제품명", example = "MCM 백팩")
        String productName,

        @Schema(description = "SKU 색상", example = "Cognac")
        String color,

        @Schema(description = "착용 요청 시 선택한 사이즈. 착용 요청이 아닌 호출은 null", example = "S-M")
        String size,

        @Schema(description = "호출 사유", example = "가격 문의")
        String reason,

        @Schema(description = "호출 상태", example = "requested")
        String status,

        @Schema(description = "요청 시각")
        LocalDateTime requestedAt,

        @Schema(description = "마지막 상태 변경 시각")
        LocalDateTime updatedAt
) {

    public static StaffCallBoardItem from(StaffCall staffCall) {
        Sku sku = staffCall.getSku();

        return new StaffCallBoardItem(
                staffCall.getCallId(),
                staffCall.getSession().getSessionId(),
                staffCall.getSession().getNickname(),
                sku != null ? sku.getProduct().getName() : null,
                sku != null ? sku.getColor() : null,
                staffCall.getSize(),
                staffCall.getReason(),
                staffCall.getStatus().name().toLowerCase(),
                staffCall.getRequestedAt(),
                staffCall.getUpdatedAt()
        );
    }
}
