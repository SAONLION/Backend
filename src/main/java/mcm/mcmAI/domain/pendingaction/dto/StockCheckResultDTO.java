package mcm.mcmAI.domain.pendingaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "타매장 재고 확인 결과 (더미 응답 - 실제 재고 조회 연동 전)")
public record StockCheckResultDTO(

        @Schema(description = "안내 메시지", example = "인근 매장 재고를 확인해드렸어요.")
        String message,

        @Schema(description = "매장명", example = "MCM 신세계 강남점")
        String storeName,

        @Schema(description = "재고 여부", example = "true")
        boolean stock
) {

    public static StockCheckResultDTO dummy() {
        return new StockCheckResultDTO("인근 매장 재고를 확인해드렸어요.", "MCM 신세계 강남점", true);
    }
}