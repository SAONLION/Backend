package mcm.mcmAI.domain.journeycard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.journeycard.dto.JourneyCardResponse;
import mcm.mcmAI.domain.journeycard.service.JourneyCardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "JourneyCard", description = "여정 카드(패스포트) API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/session/journey-card")
public class JourneyCardController {

    private final JourneyCardService journeyCardService;

    @Operation(
            summary = "여정 카드 조회",
            description = "세션이 태그 스캔한 제품들 중 관심도 점수(PurchaseInquiry/TryonRequest/Contact 발송/가격 조회/"
                    + "재방문/허브·서브허브 클릭 등을 가중합산, SKU 단위로 계산 후 같은 상품끼리 합산)가 높은 순으로 "
                    + "이미지를 콜라주 형태로 구성한 여정 카드를 반환한다. 동점이면 먼저 태그한 순서를 우선한다. "
                    + "1위 제품은 정면샷, 2위 제품은 다른 각도샷, "
                    + "3위 제품은 모델샷을 우선 사용하고(없으면 대체 샷 사용), 4번째 슬롯은 4위 제품의 이미지로 채운다. "
                    + "태그한 제품이 4개 미만이거나 이미지가 부족하면 채울 수 있는 만큼만 채우고 isComplete는 false다. "
                    + "태그 이력이 없으면 collageImages는 빈 배열이다. 세션이 존재하지 않으면 404(SESSION_NOT_FOUND)를 "
                    + "반환한다. favoriteColor는 세션 안에서 가장 많이 태그 스캔된 SKU 색상이며(동률이면 가장 최근 태그 "
                    + "색상), 태그 이력이 없으면 null이다."
    )
    @GetMapping
    public JourneyCardResponse getJourneyCard(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String sessionId
    ) {
        return journeyCardService.getJourneyCard(sessionId);
    }
}