package mcm.mcmAI.domain.recommendation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.recommendation.dto.RecommendationItem;
import mcm.mcmAI.domain.recommendation.dto.RecommendationsResponse;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.tagscanlog.entity.TagScanLog;
import mcm.mcmAI.domain.tagscanlog.repository.TagScanLogRepository;
import mcm.mcmAI.domain.visitpurpose.entity.VisitPurpose;
import mcm.mcmAI.domain.visitpurpose.repository.VisitPurposeRepository;
import mcm.mcmAI.global.ai.OpenAiClient;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private static final int CANDIDATE_LIMIT = 5;
    private static final double PRICE_BAND_RATIO = 0.3;
    private static final int VIEWED_PRODUCT_CONTEXT_LIMIT = 5;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            당신은 명품 매장의 AI 판매 어시스턴트입니다. 주어진 고객 컨텍스트와 후보 상품 목록을 보고, \
            각 후보 상품을 왜 추천하는지 1문장의 자연스러운 한국어 이유를 생성하세요.
            반드시 다음 JSON 형식으로만 응답하세요: {"items":[{"productId":number,"reason":string}]}.
            후보 상품 목록에 없는 productId를 지어내면 안 됩니다. 가능하면 모든 후보에 대해 이유를 생성하세요.""";

    private final ProductRepository productRepository;
    private final TagScanLogRepository tagScanLogRepository;
    private final VisitPurposeRepository visitPurposeRepository;
    private final SessionRepository sessionRepository;
    private final OpenAiClient openAiClient;

    public RecommendationsResponse getRecommendations(String sessionId) {
        requireSession(sessionId);

        List<TagScanLog> scanLogs = tagScanLogRepository.findBySession_SessionIdOrderByScanOrderDesc(sessionId);
        if (scanLogs.isEmpty()) {
            return new RecommendationsResponse(List.of());
        }

        List<Product> candidates = findCandidates(scanLogs);
        if (candidates.isEmpty()) {
            return new RecommendationsResponse(List.of());
        }

        VisitPurpose visitPurpose = visitPurposeRepository.findBySession_SessionId(sessionId).orElse(null);
        String customerContext = buildCustomerContext(visitPurpose, scanLogs);
        Map<Long, String> reasonsByProductId = generateReasons(candidates, customerContext);

        List<RecommendationItem> items = candidates.stream()
                .map(product -> new RecommendationItem(
                        product.getProductId(), product.getName(), reasonsByProductId.get(product.getProductId())))
                .toList();

        return new RecommendationsResponse(items);
    }

    private List<Product> findCandidates(List<TagScanLog> scanLogs) {
        Sku recentSku = scanLogs.get(0).getSku();
        Product recentProduct = recentSku.getProduct();

        List<Long> excludeProductIds = scanLogs.stream()
                .map(scanLog -> scanLog.getSku().getProduct().getProductId())
                .distinct()
                .toList();

        Integer referencePrice = recentSku.getPrice();
        int minPrice = referencePrice != null ? (int) (referencePrice * (1 - PRICE_BAND_RATIO)) : 0;
        int maxPrice = referencePrice != null ? (int) (referencePrice * (1 + PRICE_BAND_RATIO)) : Integer.MAX_VALUE;

        return productRepository.findRecommendationCandidates(
                recentProduct.getCategory(),
                excludeProductIds,
                minPrice,
                maxPrice,
                referencePrice != null ? referencePrice : 0,
                PageRequest.of(0, CANDIDATE_LIMIT)
        );
    }

    private String buildCustomerContext(VisitPurpose visitPurpose, List<TagScanLog> scanLogs) {
        StringBuilder context = new StringBuilder();
        if (visitPurpose != null) {
            context.append("방문 목적: ").append(visitPurpose.getPurposeType().getLabel()).append(". ");
        }

        String viewedProductNames = scanLogs.stream()
                .map(scanLog -> scanLog.getSku().getProduct().getName())
                .distinct()
                .limit(VIEWED_PRODUCT_CONTEXT_LIMIT)
                .collect(Collectors.joining(", "));

        if (!viewedProductNames.isBlank()) {
            context.append("최근 관심 상품: ").append(viewedProductNames).append(".");
        }

        return context.toString();
    }

    private Map<Long, String> generateReasons(List<Product> candidates, String customerContext) {
        Set<Long> candidateIds = candidates.stream().map(Product::getProductId).collect(Collectors.toSet());

        try {
            String userPrompt = buildUserPrompt(candidates, customerContext);
            return openAiClient.requestChatCompletion(SYSTEM_PROMPT, userPrompt, true)
                    .map(content -> parseReasons(content, candidateIds))
                    .orElseGet(Map::of);
        } catch (Exception e) {
            log.warn("추천 이유 생성에 실패해 기본 추천만 반환합니다: {}", e.getMessage());
            return Map.of();
        }
    }

    private String buildUserPrompt(List<Product> candidates, String customerContext) throws Exception {
        List<CandidatePromptItem> promptItems = candidates.stream()
                .map(product -> new CandidatePromptItem(
                        product.getProductId(), product.getName(), product.getCategory()))
                .toList();

        return "고객 컨텍스트: " + customerContext + "\n"
                + "후보 상품 목록: " + OBJECT_MAPPER.writeValueAsString(promptItems);
    }

    private Map<Long, String> parseReasons(String content, Set<Long> candidateIds) {
        try {
            JsonNode items = OBJECT_MAPPER.readTree(content).path("items");
            Map<Long, String> reasons = new LinkedHashMap<>();
            for (JsonNode item : items) {
                long productId = item.path("productId").asLong();
                String reason = item.path("reason").asText(null);
                if (reason != null && !reason.isBlank() && candidateIds.contains(productId)) {
                    reasons.put(productId, reason);
                }
            }
            return reasons;
        } catch (Exception e) {
            log.warn("OpenAI 응답 파싱에 실패했습니다: {}", e.getMessage());
            return Map.of();
        }
    }

    private record CandidatePromptItem(Long productId, String name, String category) {
    }

    private void requireSession(String sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }
    }
}