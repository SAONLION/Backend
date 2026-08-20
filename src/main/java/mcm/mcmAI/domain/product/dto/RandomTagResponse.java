package mcm.mcmAI.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카테고리별 랜덤 태그 선택 응답")
public record RandomTagResponse(

        @Schema(description = "무작위로 선택된 태그(SKU) ID. GET /api/v1/products/tags/{tagId}로 바로 조회할 수 있다. "
                + "조건에 맞는 SKU가 없으면 null이다.", example = "156")
        Long tagId
) {
}
