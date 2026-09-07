package mcm.mcmAI.internal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.product.dto.ProductTagScanResponseDTO;
import mcm.mcmAI.domain.product.service.ProductService;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import mcm.mcmAI.domain.tagscanlog.repository.TagScanLogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "InternalTest", description = "[시연/테스트 전용] 정식 기능 아님. app.internal-test-endpoints.enabled=true 일 때만 활성화된다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/test/products")
@ConditionalOnProperty(prefix = "app.internal-test-endpoints", name = "enabled", havingValue = "true")
public class InternalTestProductController {

    private final ProductService productService;
    private final TagScanLogRepository tagScanLogRepository;
    private final SkuRepository skuRepository;

    @Operation(
            summary = "[테스트 전용] 랜덤 태그 스캔",
            description = "물리 NFC 태그가 아직 없는 시연 환경에서, 카탈로그 전체 SKU 중 하나를 골라 실제 태그 스캔과 "
                    + "동일하게 처리한다. 첫 스캔을 포함해 매번 카테고리 구분 없이 카탈로그 전체 SKU 중에서 완전 "
                    + "무작위로 고른다. 이미 세션에서 태그된(tag_scan_log에 기록된) SKU는 후보에서 제외되며, 카탈로그 "
                    + "전체를 다 태그해 더 고를 후보가 없으면 제외 없이 처음부터 다시 순환한다. 응답 형식과 "
                    + "tag_scan_log 기록은 GET /api/v1/products/tags/{tagId}와 완전히 동일하다. 정식 기능이 아니며 "
                    + "인증이 필요 없다. app.internal-test-endpoints.enabled=true(기본값 false)일 때만 이 "
                    + "엔드포인트가 등록되며, 꺼져 있으면 404를 반환한다."
    )
    @GetMapping("/random-tag")
    public ProductTagScanResponseDTO scanRandomTag(
            @Parameter(description = "스캔을 수행한 세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String sessionId
    ) {
        Long tagId = pickNextDemoTagId(sessionId);
        return productService.getProductByTag(tagId, sessionId);
    }

    private Long pickNextDemoTagId(String sessionId) {
        Set<Long> usedTagIds = tagScanLogRepository.findBySession_SessionIdOrderByScanOrderDesc(sessionId).stream()
                .map(scanLog -> scanLog.getSku().getSku())
                .collect(Collectors.toSet());

        List<Long> allTagIds = skuRepository.findAllActiveWithProduct().stream()
                .map(Sku::getSku)
                .toList();

        List<Long> remaining = allTagIds.stream().filter(id -> !usedTagIds.contains(id)).toList();

        // 카탈로그 전체를 다 태그해 더 이상 새로 보여줄 상품이 없으면, 시연이 끊기지 않도록 제외 없이 처음부터 다시 순환한다.
        List<Long> candidates = remaining.isEmpty() ? allTagIds : remaining;

        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }
}
