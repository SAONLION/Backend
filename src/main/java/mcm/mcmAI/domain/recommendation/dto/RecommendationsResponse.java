package mcm.mcmAI.domain.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "추천 제품 목록 응답")
public record RecommendationsResponse(

        @Schema(description = "추천 제품 목록 (최대 3개, 스캔 이력이 없으면 빈 배열)")
        List<RecommendationItem> recommendations
) {
}