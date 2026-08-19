package mcm.mcmAI.domain.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import mcm.mcmAI.domain.recommendation.type.RecommendationStatus;

@Schema(description = "추천 제품 목록 응답")
public record RecommendationsResponse(

        @Schema(description = "추천 상태. 후보를 하나도 채우지 못해 recommendations가 비어 있으면 NO_CANDIDATE, "
                + "그 외에는 OK다.", example = "OK")
        RecommendationStatus status,

        @Schema(description = "추천 제품 목록 (정상적으로는 항상 3개. 카탈로그에 채울 후보가 전혀 없을 때만 "
                + "3개보다 적거나 빈 배열일 수 있다)")
        List<RecommendationItem> recommendations
) {

    public static RecommendationsResponse of(List<RecommendationItem> recommendations) {
        RecommendationStatus status = recommendations.isEmpty()
                ? RecommendationStatus.NO_CANDIDATE
                : RecommendationStatus.OK;
        return new RecommendationsResponse(status, recommendations);
    }
}
