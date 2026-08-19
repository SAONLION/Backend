package mcm.mcmAI.domain.recommendation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcm.mcmAI.domain.interactionlog.entity.InteractionLog;
import mcm.mcmAI.domain.interactionlog.repository.InteractionLogRepository;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.recommendation.dto.RecommendationItem;
import mcm.mcmAI.domain.recommendation.dto.RecommendationsResponse;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import mcm.mcmAI.domain.skuimage.repository.SkuImageRepository;
import mcm.mcmAI.domain.skuimage.type.ShotType;
import mcm.mcmAI.domain.tagscanlog.entity.TagScanLog;
import mcm.mcmAI.domain.tagscanlog.repository.TagScanLogRepository;
import mcm.mcmAI.domain.visitpurpose.entity.VisitPurpose;
import mcm.mcmAI.domain.visitpurpose.repository.VisitPurposeRepository;
import mcm.mcmAI.global.ai.CosineSimilarity;
import mcm.mcmAI.global.ai.EmbeddingCodec;
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

    private static final int RECOMMENDATION_LIMIT = 3;
    private static final double PRICE_BAND_RATIO = 0.3;
    private static final int VIEWED_PRODUCT_CONTEXT_LIMIT = 5;
    private static final long NO_EXCLUSION_SENTINEL_ID = 0L;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            당신은 명품 매장의 AI 판매 어시스턴트입니다. 주어진 고객 세션 맥락과 후보 상품 목록을 보고, \
            각 후보 상품을 왜 추천하는지 1문장의 자연스러운 한국어 이유를 생성하세요.
            반드시 다음 JSON 형식으로만 응답하세요: {"items":[{"productId":number,"reason":string}]}.
            후보 상품 목록에 없는 productId를 지어내면 안 됩니다. 가능하면 모든 후보에 대해 이유를 생성하세요.""";

    private final ProductRepository productRepository;
    private final TagScanLogRepository tagScanLogRepository;
    private final VisitPurposeRepository visitPurposeRepository;
    private final InteractionLogRepository interactionLogRepository;
    private final SessionRepository sessionRepository;
    private final SkuRepository skuRepository;
    private final SkuImageRepository skuImageRepository;
    private final OpenAiClient openAiClient;
    private final EmbeddingCodec embeddingCodec;

    public RecommendationsResponse getRecommendations(String sessionId) {
        requireSession(sessionId);

        List<TagScanLog> scanLogs = tagScanLogRepository.findBySession_SessionIdOrderByScanOrderDesc(sessionId);
        Set<Long> excludeProductIds = scanLogs.stream()
                .map(scanLog -> scanLog.getSku().getProduct().getProductId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (scanLogs.isEmpty()) {
            // 태그 이력이 전혀 없어 개인화 기준(최근 카테고리/가격대)이 없으므로, 전체 인기 상품으로 채운다.
            List<Product> candidates = productRepository.findPopularProductCandidates(
                    toExcludeParam(excludeProductIds), PageRequest.of(0, RECOMMENDATION_LIMIT));
            return buildResponse(candidates, Map.of());
        }

        VisitPurpose visitPurpose = visitPurposeRepository.findBySession_SessionId(sessionId).orElse(null);
        List<InteractionLog> interactionLogs = interactionLogRepository.findBySession_SessionId(sessionId);
        String sessionContext = buildSessionContext(visitPurpose, scanLogs, interactionLogs);

        TagScanLog mostRecentScan = scanLogs.get(0);

        Optional<List<Product>> vectorCandidates = findSimilarCandidates(sessionContext, excludeProductIds);
        if (vectorCandidates.isPresent()) {
            Optional<Map<Long, String>> reasonsByProductId = generateReasons(vectorCandidates.get(), sessionContext);
            if (reasonsByProductId.isPresent()) {
                // AI가 선별한 후보가 3개 미만이면, 규칙 완화·인기 상품 순으로 나머지를 채운다(이유는 채워진 만큼만 존재).
                List<Product> candidates = fillToLimit(vectorCandidates.get(), excludeProductIds, mostRecentScan);
                return buildResponse(candidates, reasonsByProductId.get());
            }
        }

        // 임베딩/이유 생성이 불가능하거나 실패하면, AI 후보는 신뢰하지 않고 규칙 기반(카테고리+가격대 → 완화 → 인기)으로만 채운다.
        List<Product> candidates = fillToLimit(List.of(), excludeProductIds, mostRecentScan);
        return buildResponse(candidates, Map.of());
    }

    // 세션 벡터를 생성하고, 이미 태그된 제품을 제외한 임베딩 보유 제품들과 코사인 유사도를 계산해 상위 N개를 고른다.
    // 임베딩 호출이 실패했거나(키 미설정/네트워크 오류) 비교할 벡터가 하나도 없으면 빈 Optional을 반환한다.
    private Optional<List<Product>> findSimilarCandidates(String sessionContext, Set<Long> excludeProductIds) {
        Optional<float[]> sessionVectorOpt;
        try {
            sessionVectorOpt = openAiClient.requestEmbedding(sessionContext);
        } catch (Exception e) {
            log.warn("세션 임베딩 생성에 실패했습니다: {}", e.getMessage());
            return Optional.empty();
        }
        if (sessionVectorOpt.isEmpty()) {
            return Optional.empty();
        }
        float[] sessionVector = sessionVectorOpt.get();

        List<Product> vectorizedProducts =
                productRepository.findByEmbeddingIsNotNullAndProductIdNotIn(excludeProductIds);
        if (vectorizedProducts.isEmpty()) {
            return Optional.empty();
        }

        List<ScoredProduct> scoredProducts = new ArrayList<>();
        for (Product product : vectorizedProducts) {
            float[] productVector = embeddingCodec.decode(product.getEmbedding());
            if (productVector == null || productVector.length != sessionVector.length) {
                continue;
            }
            scoredProducts.add(new ScoredProduct(product, CosineSimilarity.compute(sessionVector, productVector)));
        }
        if (scoredProducts.isEmpty()) {
            return Optional.empty();
        }

        List<Product> topCandidates = scoredProducts.stream()
                .sorted(Comparator.comparingDouble(ScoredProduct::similarity).reversed())
                .limit(RECOMMENDATION_LIMIT)
                .map(ScoredProduct::product)
                .toList();

        return Optional.of(topCandidates);
    }

    // seed(있으면 AI가 고른 후보)를 시작으로, 부족한 만큼 카테고리+가격대 규칙 → 가격대 완화 → 전체 인기 상품 순으로 채운다.
    // 각 단계는 이전 단계에서 고른 후보와 이미 태그된 제품을 제외하며, 어느 단계에서도 부족분만큼만 조회한다.
    private List<Product> fillToLimit(List<Product> seed, Set<Long> excludeProductIds, TagScanLog mostRecentScan) {
        List<Product> result = new ArrayList<>(seed);
        if (result.size() >= RECOMMENDATION_LIMIT) {
            return result;
        }

        Set<Long> seen = new LinkedHashSet<>(excludeProductIds);
        seed.forEach(product -> seen.add(product.getProductId()));

        Sku recentSku = mostRecentScan.getSku();
        Product recentProduct = recentSku.getProduct();
        int referencePrice = recentSku.getPrice() != null ? recentSku.getPrice() : 0;
        int minPrice = (int) (referencePrice * (1 - PRICE_BAND_RATIO));
        int maxPrice = (int) (referencePrice * (1 + PRICE_BAND_RATIO));

        appendUnseen(result, seen, productRepository.findRecommendationCandidates(
                recentProduct.getCategory(), toExcludeParam(seen), minPrice, maxPrice, referencePrice,
                PageRequest.of(0, RECOMMENDATION_LIMIT - result.size())));

        if (result.size() < RECOMMENDATION_LIMIT) {
            appendUnseen(result, seen, productRepository.findRecommendationCandidates(
                    recentProduct.getCategory(), toExcludeParam(seen), 0, Integer.MAX_VALUE, referencePrice,
                    PageRequest.of(0, RECOMMENDATION_LIMIT - result.size())));
        }

        if (result.size() < RECOMMENDATION_LIMIT) {
            appendUnseen(result, seen, productRepository.findPopularProductCandidates(
                    toExcludeParam(seen), PageRequest.of(0, RECOMMENDATION_LIMIT - result.size())));
        }

        return result;
    }

    private void appendUnseen(List<Product> result, Set<Long> seen, List<Product> newCandidates) {
        for (Product candidate : newCandidates) {
            if (seen.add(candidate.getProductId())) {
                result.add(candidate);
            }
        }
    }

    // 네이티브 쿼리의 NOT IN(:excludeProductIds)에 빈 컬렉션을 바인딩하면 SQL 문법 오류가 나므로,
    // 존재할 수 없는 ID(0)로 대체해 "제외할 것이 없다"를 표현한다.
    private List<Long> toExcludeParam(Set<Long> excludeProductIds) {
        return excludeProductIds.isEmpty() ? List.of(NO_EXCLUSION_SENTINEL_ID) : List.copyOf(excludeProductIds);
    }

    // AI가 아닌 순수 텍스트 조합: 방문 목적 + 태그한 제품명 + interaction_log 기반 관심사를 한 문장으로 합친다.
    private String buildSessionContext(
            VisitPurpose visitPurpose, List<TagScanLog> scanLogs, List<InteractionLog> interactionLogs
    ) {
        StringBuilder context = new StringBuilder("이 고객은 ");

        if (visitPurpose != null) {
            context.append(visitPurpose.getPurposeType().getLabel()).append(" 목적으로 방문했고, ");
        }

        String taggedProductNames = scanLogs.stream()
                .map(scanLog -> scanLog.getSku().getProduct().getName())
                .distinct()
                .limit(VIEWED_PRODUCT_CONTEXT_LIMIT)
                .collect(Collectors.joining(", "));
        if (!taggedProductNames.isBlank()) {
            context.append(taggedProductNames).append("을(를) 태그했으며, ");
        }

        String topInterest = findTopInterest(interactionLogs);
        if (topInterest != null) {
            context.append(topInterest).append("을(를) 오래 살펴보았다.");
        } else {
            if (context.toString().endsWith(", ")) {
                context.setLength(context.length() - 2);
            }
            context.append('.');
        }

        return context.toString();
    }

    // 상세 옵션(subOption, 예: 소재)이 있으면 그것을, 없으면 제품명을 기준으로 관심 시간을 합산해 가장 오래 본 대상을 찾는다.
    private String findTopInterest(List<InteractionLog> interactionLogs) {
        if (interactionLogs.isEmpty()) {
            return null;
        }

        Map<String, Integer> durationByLabel = new LinkedHashMap<>();
        for (InteractionLog interactionLog : interactionLogs) {
            String label = (interactionLog.getSubOption() != null && !interactionLog.getSubOption().isBlank())
                    ? interactionLog.getSubOption()
                    : interactionLog.getSku().getProduct().getName();
            int duration = interactionLog.getDurationSeconds() != null ? interactionLog.getDurationSeconds() : 0;
            durationByLabel.merge(label, duration, Integer::sum);
        }

        return durationByLabel.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    // 후보(최대 3개) + 세션 맥락을 LLM에 전달해 이유를 생성한다. 호출/파싱이 실패하면 빈 Optional(=fallback 신호)을 반환한다.
    private Optional<Map<Long, String>> generateReasons(List<Product> candidates, String sessionContext) {
        Set<Long> candidateIds = candidates.stream().map(Product::getProductId).collect(Collectors.toSet());

        Optional<String> content;
        try {
            String userPrompt = buildUserPrompt(candidates, sessionContext);
            content = openAiClient.requestChatCompletion(SYSTEM_PROMPT, userPrompt, true);
        } catch (Exception e) {
            log.warn("추천 이유 생성 요청에 실패했습니다: {}", e.getMessage());
            return Optional.empty();
        }
        if (content.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(parseReasons(content.get(), candidateIds));
        } catch (Exception e) {
            log.warn("OpenAI 응답 파싱에 실패했습니다: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String buildUserPrompt(List<Product> candidates, String sessionContext) throws Exception {
        List<CandidatePromptItem> promptItems = candidates.stream()
                .map(product -> new CandidatePromptItem(
                        product.getProductId(), product.getName(), product.getCategory()))
                .toList();

        return "고객 세션 맥락: " + sessionContext + "\n"
                + "후보 상품 목록: " + OBJECT_MAPPER.writeValueAsString(promptItems);
    }

    // 환각 방지: 응답 JSON 자체는 유효하되, 후보 목록에 없는 productId가 섞여 있으면 그 항목만 걸러낸다.
    private Map<Long, String> parseReasons(String content, Set<Long> candidateIds) throws Exception {
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
    }

    // 후보 상품마다 대표 SKU(최소 SKU ID)와 그 SKU의 대표 이미지(PRODUCT 샷, position 오름차순 첫 장)를 붙여 응답을 만든다.
    private RecommendationsResponse buildResponse(List<Product> candidates, Map<Long, String> reasonsByProductId) {
        List<RecommendationItem> items = candidates.stream()
                .map(product -> toRecommendationItem(product, reasonsByProductId.get(product.getProductId())))
                .toList();
        return RecommendationsResponse.of(items);
    }

    private RecommendationItem toRecommendationItem(Product product, String reason) {
        Optional<Sku> representativeSku =
                skuRepository.findByProduct_ProductIdOrderBySkuAsc(product.getProductId()).stream().findFirst();

        Long skuId = representativeSku.map(Sku::getSku).orElse(null);
        String imageUrl = representativeSku
                .map(Sku::getStyleNumber)
                .flatMap(styleNumber ->
                        skuImageRepository.findFirstByStyleNumberAndShotTypeOrderByPositionAsc(
                                styleNumber, ShotType.PRODUCT))
                .map(skuImage -> skuImage.getImageUrl())
                .orElse(null);

        return new RecommendationItem(product.getProductId(), skuId, product.getName(), imageUrl, reason);
    }

    private record CandidatePromptItem(Long productId, String name, String category) {
    }

    private record ScoredProduct(Product product, double similarity) {
    }

    private void requireSession(String sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }
    }
}
