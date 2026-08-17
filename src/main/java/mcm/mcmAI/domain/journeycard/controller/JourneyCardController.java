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
            description = "세션이 태그 스캔한 제품들의 이미지를 콜라주 형태로 구성한 여정 카드를 반환한다. "
                    + "모델샷 1장 + 제품샷 3장(총 4장)을 목표로 하되, 모델샷이 없는 제품만 태그했다면 제품샷으로 "
                    + "채운다. 여러 제품을 태그했으면 한 제품에 몰리지 않도록 태그 순서대로 골고루 섞는다. "
                    + "태그 이력이 없으면 collageImages는 빈 배열이다. 세션이 존재하지 않으면 404(SESSION_NOT_FOUND)를 "
                    + "반환한다."
    )
    @GetMapping
    public JourneyCardResponse getJourneyCard(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String sessionId
    ) {
        return journeyCardService.getJourneyCard(sessionId);
    }
}