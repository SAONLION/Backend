package mcm.mcmAI.domain.recommendation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.recommendation.dto.RecommendationsResponse;
import mcm.mcmAI.domain.recommendation.service.RecommendationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recommendation", description = "AI 추천 제품 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/session/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(
            summary = "추천 제품 목록 조회",
            description = "세션의 방문목적(visit_purpose), 태그 스캔 이력(tag_scan_log), 상호작용 로그(interaction_log)를 "
                    + "한 문장의 세션 맥락으로 요약하고, 이를 OpenAI 임베딩(text-embedding-3-small)으로 벡터화한 뒤 "
                    + "미리 생성해둔 제품 벡터들과 코사인 유사도를 계산해 상위 3개를 후보로 고른다(이미 태그된 제품은 "
                    + "제외). 이후 OpenAI(gpt-4o-mini)를 호출해 후보별 추천 이유 문구를 생성한다. AI 응답에 후보 "
                    + "목록에 없는 productId가 포함되면 걸러낸다. OPENAI_API_KEY가 없거나 임베딩/이유 생성 호출·파싱에 "
                    + "실패하면 500 에러 대신 카테고리+가격대(±30%) 규칙으로 후보를 추린다. 어느 단계에서든 후보가 "
                    + "3개 미만이면 가격대 제한을 완화하고, 그래도 부족하면 전체 세션에서 가장 많이 태그된 인기 상품 "
                    + "순으로 채워 recommendations는 항상 정확히 3개를 유지한다(채워진 후보는 reason이 null이다). "
                    + "태그 스캔 이력이 없으면 인기 상품만으로 채운다. 카탈로그에 채울 후보가 정말 하나도 없을 때만 "
                    + "recommendations가 3개보다 적거나 빈 배열이 되며, 이 경우 status가 NO_CANDIDATE다(정상은 OK). "
                    + "세션이 존재하지 않으면 404(SESSION_NOT_FOUND)를 반환한다."
    )
    @GetMapping
    public RecommendationsResponse getRecommendations(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String sessionId
    ) {
        return recommendationService.getRecommendations(sessionId);
    }
}