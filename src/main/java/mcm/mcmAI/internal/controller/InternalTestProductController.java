package mcm.mcmAI.internal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    // 시연 첫 턴에 무조건 노출할 대표 상품(비세토스 백팩, bags_all 카테고리)의 SKU(=태그) ID.
    private static final long FIRST_TURN_TAG_ID = 1L;

    private final ProductService productService;
    private final TagScanLogRepository tagScanLogRepository;
    private final SkuRepository skuRepository;

    @Operation(
            summary = "[테스트 전용] 랜덤 태그 스캔",
            description = "물리 NFC 태그가 아직 없는 시연 환경에서, 카탈로그 전체 SKU 중 하나를 골라 실제 태그 스캔과 "
                    + "동일하게 처리한다. 세션의 첫 스캔이면 항상 비세토스 백팩(bags_all)이 나오고, 이후에는 "
                    + "product.category 값에 'bag'이 포함되면 가방, 'wallet'이 포함되면 지갑, 그 외에는 기타로 분류한 "
                    + "뒤 가방 50% / 지갑 30% / 기타 20% 가중치로 카테고리를 고르고 그 카테고리 안의 모든 SKU 중에서 "
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

        if (usedTagIds.isEmpty()) {
            return FIRST_TURN_TAG_ID;
        }

        Map<DemoCategory, List<Long>> tagIdsByCategory = loadTagIdsByCategory();

        Map<DemoCategory, List<Long>> remainingByCategory = new EnumMap<>(DemoCategory.class);
        tagIdsByCategory.forEach((category, tagIds) -> {
            List<Long> remaining = tagIds.stream().filter(id -> !usedTagIds.contains(id)).toList();
            if (!remaining.isEmpty()) {
                remainingByCategory.put(category, remaining);
            }
        });

        // 카탈로그 전체를 다 태그해 더 이상 새로 보여줄 상품이 없으면, 시연이 끊기지 않도록 제외 없이 처음부터 다시 순환한다.
        if (remainingByCategory.isEmpty()) {
            remainingByCategory.putAll(tagIdsByCategory);
        }

        List<Long> candidates = remainingByCategory.get(pickWeightedCategory(remainingByCategory.keySet()));
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    // 카탈로그 전체 활성 SKU를 product.category 기준으로 가방/지갑/기타 3버킷으로 분류한다.
    private Map<DemoCategory, List<Long>> loadTagIdsByCategory() {
        return skuRepository.findAllActiveWithProduct().stream()
                .collect(Collectors.groupingBy(
                        sku -> classify(sku.getProduct().getCategory()),
                        () -> new EnumMap<>(DemoCategory.class),
                        Collectors.mapping(Sku::getSku, Collectors.toList())
                ));
    }

    private static DemoCategory classify(String category) {
        if (category == null) {
            return DemoCategory.OTHER;
        }
        String normalized = category.toLowerCase(Locale.ROOT);
        if (normalized.contains("wallet")) {
            return DemoCategory.WALLET;
        }
        if (normalized.contains("bag")) {
            return DemoCategory.BAG;
        }
        return DemoCategory.OTHER;
    }

    private DemoCategory pickWeightedCategory(Set<DemoCategory> availableCategories) {
        int totalWeight = availableCategories.stream().mapToInt(category -> category.weight).sum();
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);

        int cumulative = 0;
        for (DemoCategory category : DemoCategory.values()) {
            if (!availableCategories.contains(category)) {
                continue;
            }
            cumulative += category.weight;
            if (roll < cumulative) {
                return category;
            }
        }
        throw new IllegalStateException("가중치 누적 합이 총 가중치에 도달하지 못했습니다.");
    }

    // 카탈로그 SKU를 노출 비중별로 묶은 카테고리. 가방을 메인으로 두고 가방 50% / 지갑 30% / 기타 20% 가중치를 둔다.
    private enum DemoCategory {
        BAG(50),
        WALLET(30),
        OTHER(20);

        private final int weight;

        DemoCategory(int weight) {
            this.weight = weight;
        }
    }
}
