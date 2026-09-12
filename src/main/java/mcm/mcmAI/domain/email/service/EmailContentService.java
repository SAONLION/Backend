package mcm.mcmAI.domain.email.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.email.dto.EmailContentResponse;
import mcm.mcmAI.domain.email.dto.EmailSlotItem;
import mcm.mcmAI.domain.email.type.SlotType;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.recommendation.dto.RecommendationItem;
import mcm.mcmAI.domain.recommendation.service.RecommendationService;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import mcm.mcmAI.domain.skuimage.repository.SkuImageRepository;
import mcm.mcmAI.domain.skuimage.type.ShotType;
import mcm.mcmAI.domain.tagscanlog.entity.TagScanLog;
import mcm.mcmAI.domain.tagscanlog.repository.TagScanLogRepository;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * email_content.html의 치환 토큰을 ERD에서 끌어와 채운다.
 * 렌더링과 발송은 하지 않으므로, 미리보기(QA)와 실제 발송이 완전히 같은 데이터를 보게 된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailContentService {

    private static final int PICK_SLOT_COUNT = 4;
    private static final int RECOMMEND_SLOT_COUNT = 2;

    private final SessionRepository sessionRepository;
    private final TagScanLogRepository tagScanLogRepository;
    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    private final SkuImageRepository skuImageRepository;
    private final RecommendationService recommendationService;
    private final EmailProperties emailProperties;

    public EmailContentResponse buildContent(String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        List<EmailSlotItem> picks = buildPicks(sessionId);
        List<EmailSlotItem> recommendations = buildRecommendations(sessionId);

        return new EmailContentResponse(
                sessionId,
                resolveNickname(session),
                emailProperties.getStoreName(),
                picks,
                recommendations,
                (int) picks.stream().filter(EmailSlotItem::isFilled).count(),
                emailProperties.getAssetBaseUrl(),
                emailProperties.getUnsubscribeUrl(),
                emailProperties.getPrivacyUrl()
        );
    }

    private String resolveNickname(Session session) {
        String nickname = session.getNickname();
        return nickname == null || nickname.isBlank() ? emailProperties.getDefaultNickname() : nickname;
    }

    /**
     * 고객이 태그한 상품으로 PICK 4칸을 채운다. 같은 SKU를 여러 번 태그했어도 카드는 한 번만 쓰고,
     * 태그가 4개를 넘으면 최근 4개만 남긴 뒤 실제 태그한 순서(scan_order 오름차순)로 되돌려 배치한다.
     * 템플릿 레이아웃이 고정 4칸이라 부족한 자리는 빈 슬롯으로 채워 길이를 항상 4로 맞춘다.
     */
    private List<EmailSlotItem> buildPicks(String sessionId) {
        Map<Long, TagScanLog> recentScansBySku = new LinkedHashMap<>();
        for (TagScanLog scanLog : tagScanLogRepository.findBySession_SessionIdOrderByScanOrderDesc(sessionId)) {
            if (scanLog.getSku() == null) {
                continue;
            }
            recentScansBySku.putIfAbsent(scanLog.getSku().getSku(), scanLog);
            if (recentScansBySku.size() >= PICK_SLOT_COUNT) {
                break;
            }
        }

        List<TagScanLog> orderedScans = new ArrayList<>(recentScansBySku.values());
        orderedScans.sort(Comparator.comparing(
                TagScanLog::getScanOrder, Comparator.nullsLast(Comparator.naturalOrder())));

        List<EmailSlotItem> picks = new ArrayList<>();
        for (TagScanLog scanLog : orderedScans) {
            int slotOrder = picks.size() + 1;
            // 태그 이후 SKU/상품이 삭제됐을 수 있다. 그런 칸은 억지로 채우지 않고 빈 슬롯으로 둔다.
            toSlotItem(scanLog.getSku().getSku(), SlotType.PICK, slotOrder)
                    .ifPresent(picks::add);
        }

        return padToSize(picks, SlotType.PICK, PICK_SLOT_COUNT);
    }

    /** 추천 2칸은 기존 추천 로직(AI 임베딩 → 규칙 폴백)의 상위 결과를 그대로 쓴다. */
    private List<EmailSlotItem> buildRecommendations(String sessionId) {
        List<RecommendationItem> items = recommendationService.getRecommendations(sessionId).recommendations();

        List<EmailSlotItem> recommendations = new ArrayList<>();
        for (RecommendationItem item : items) {
            if (recommendations.size() >= RECOMMEND_SLOT_COUNT) {
                break;
            }
            if (item.skuId() == null) {
                continue;
            }
            recommendations.add(new EmailSlotItem(
                    SlotType.RECOMMEND,
                    recommendations.size() + 1,
                    item.skuId(),
                    item.productId(),
                    item.productName(),
                    resolveDescription(item.productId()),
                    item.imageUrl() != null ? item.imageUrl() : emailProperties.getPlaceholderImageUrl()
            ));
        }

        return padToSize(recommendations, SlotType.RECOMMEND, RECOMMEND_SLOT_COUNT);
    }

    /** {{recommendDescN}}에 들어갈 한 줄 설명. 소재 설명을 우선하고, 없으면 헤리티지 설명으로 대체한다. */
    private String resolveDescription(Long productId) {
        if (productId == null) {
            return "";
        }
        return productRepository.findById(productId)
                .map(this::resolveDescription)
                .orElse("");
    }

    private String resolveDescription(Product product) {
        if (product.getMaterialDesc() != null && !product.getMaterialDesc().isBlank()) {
            return product.getMaterialDesc();
        }
        return product.getHeritageDesc() != null ? product.getHeritageDesc() : "";
    }

    private Optional<EmailSlotItem> toSlotItem(Long skuId, SlotType slotType, int slotOrder) {
        Optional<Sku> skuOpt = skuRepository.findBySkuAndIsDeletedFalse(skuId);
        if (skuOpt.isEmpty() || skuOpt.get().getProduct() == null) {
            return Optional.empty();
        }
        Sku sku = skuOpt.get();

        // Sku.product는 지연 로딩 프록시라, 상품 행이 이미 삭제됐으면 이름 접근 시점에 예외가 난다.
        // 프록시를 건드리지 않고 실제 조회로 존재 여부부터 확인한다.
        Optional<Product> product = productRepository.findById(sku.getProduct().getProductId());
        if (product.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new EmailSlotItem(
                slotType,
                slotOrder,
                sku.getSku(),
                product.get().getProductId(),
                product.get().getName(),
                resolveDescription(product.get()),
                resolveImageUrl(sku)
        ));
    }

    /** 대표 이미지는 추천 API와 같은 기준(PRODUCT 샷, position 오름차순 첫 장)을 쓴다. */
    private String resolveImageUrl(Sku sku) {
        if (sku.getStyleNumber() == null) {
            return emailProperties.getPlaceholderImageUrl();
        }
        return skuImageRepository
                .findFirstByStyleNumberAndShotTypeAndIsDeletedFalseOrderByPositionAsc(
                        sku.getStyleNumber(), ShotType.PRODUCT)
                .map(skuImage -> skuImage.getImageUrl())
                .orElse(emailProperties.getPlaceholderImageUrl());
    }

    private List<EmailSlotItem> padToSize(List<EmailSlotItem> slots, SlotType slotType, int size) {
        List<EmailSlotItem> padded = new ArrayList<>(slots);
        while (padded.size() < size) {
            padded.add(EmailSlotItem.empty(slotType, padded.size() + 1, emailProperties.getPlaceholderImageUrl()));
        }
        return List.copyOf(padded);
    }
}
