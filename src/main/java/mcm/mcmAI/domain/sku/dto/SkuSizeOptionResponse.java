package mcm.mcmAI.domain.sku.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사이즈 코드별 치수")
public record SkuSizeOptionResponse(

        @Schema(description = "사이즈 코드 (size 필드의 콤마 구분 항목과 동일한 표기)", example = "41C")
        String code,

        @Schema(description = "이 사이즈 코드의 가로/세로/폭 치수 안내. 원본 데이터가 사이즈별로 구분되어 있지 않아 "
                + "이 코드에 해당하는 값을 확정할 수 없으면 null이다.", example = "약 17 x 41 x 26 센티미터", nullable = true)
        String dimensions
) {
}
