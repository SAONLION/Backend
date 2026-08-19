package mcm.mcmAI.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "2차 세부 옵션")
public record SubOptionDTO(

        @Schema(description = "세부 옵션 ID", example = "1")
        Long id,

        @Schema(description = "화면에 노출되는 라벨", example = "소재 안내")
        String label,

        @Schema(description = "세부 옵션 타입 코드", example = "MATERIAL")
        String type,

        @Schema(description = "처리 방식 (INFO: 바로 내용 노출, STAFF_MEDIATED: 다음 단계 안내)", example = "STAFF_MEDIATED", nullable = true)
        String mediation
) {
}