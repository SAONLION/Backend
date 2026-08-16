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
            description = "세션의 방문목적(visit_purpose)과 태그 스캔 이력(tag_scan_log)을 바탕으로 다음 추천 제품 "
                    + "목록을 반환한다. 가장 최근에 스캔한 제품과 같은 카테고리·비슷한 가격대(스캔가 ±30%) 제품 중 "
                    + "최대 5개를 규칙 기반으로 먼저 추린 뒤, OpenAI(gpt-4o-mini)를 호출해 후보별 추천 이유 문구를 "
                    + "생성한다. AI 응답에 후보 목록에 없는 productId가 포함되면 걸러낸다. 아직 태그 스캔 이력이 "
                    + "없으면 빈 목록을 반환하며, OPENAI_API_KEY가 없거나 AI 호출/파싱에 실패해도 500 에러 대신 "
                    + "reason 없이 기본 추천 목록만 반환한다. 세션이 존재하지 않으면 404(SESSION_NOT_FOUND)를 "
                    + "반환한다."
    )
    @GetMapping
    public RecommendationsResponse getRecommendations(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String sessionId
    ) {
        return recommendationService.getRecommendations(sessionId);
    }
}