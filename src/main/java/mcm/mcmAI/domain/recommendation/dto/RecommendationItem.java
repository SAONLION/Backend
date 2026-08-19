package mcm.mcmAI.domain.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "추천 제품 항목")
public record RecommendationItem(

        @Schema(description = "상품 ID", example = "101")
        Long productId,

        @Schema(description = "대표 SKU ID. 상품에 SKU가 하나도 없으면 null이다.", example = "1")
        Long skuId,

        @Schema(description = "상품명", example = "비세토스 백팩 M 코냑")
        String productName,

        @Schema(description = "대표 이미지 URL. 대표 SKU가 없거나 등록된 이미지가 없으면 null이다.",
                example = "https://cdn.example.com/sku/1/product-1.jpg")
        String imageUrl,

        @Schema(description = "AI가 생성한 추천 이유. OPENAI_API_KEY가 없거나 AI 호출/검증에 실패했거나, "
                + "규칙 완화·인기 상품으로 채워진 후보라 AI가 이유를 생성하지 않았으면 null이다.",
                example = "고객님이 관심 보이신 여행용 가방과 비슷한 스타일이면서, 더 컴팩트한 사이즈예요.")
        String reason
) {
}
