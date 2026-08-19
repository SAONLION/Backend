package mcm.mcmAI.domain.journeycard.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcm.mcmAI.domain.contact.entity.Contact;
import mcm.mcmAI.domain.contact.repository.ContactRepository;
import mcm.mcmAI.domain.interactionlog.entity.InteractionLog;
import mcm.mcmAI.domain.interactionlog.repository.InteractionLogRepository;
import mcm.mcmAI.domain.journeycard.dto.CollageImageResponse;
import mcm.mcmAI.domain.journeycard.dto.JourneyCardResponse;
import mcm.mcmAI.domain.product.type.InterestType;
import mcm.mcmAI.domain.purchaseinquiry.entity.PurchaseInquiry;
import mcm.mcmAI.domain.purchaseinquiry.repository.PurchaseInquiryRepository;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.skuimage.entity.SkuImage;
import mcm.mcmAI.domain.skuimage.repository.SkuImageRepository;
import mcm.mcmAI.domain.skuimage.type.ShotType;
import mcm.mcmAI.domain.tagscanlog.entity.TagScanLog;
import mcm.mcmAI.domain.tagscanlog.repository.TagScanLogRepository;
import mcm.mcmAI.domain.tryonrequest.entity.TryonRequest;
import mcm.mcmAI.domain.tryonrequest.repository.TryonRequestRepository;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JourneyCardService {

    private static final String BRAND = "MCM";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yy/MM/dd");
    private static final int SESSION_CODE_LENGTH = 5;
    private static final int TARGET_TOTAL_COUNT = 4;

    // 관심도 슬롯 역할: 0=1위(정면샷), 1=2위(다른 각도), 2=3위(모델샷 우선), 그 이후=컨셉샷(4위 이하 대체)
    private static final int ROLE_FRONT_SHOT = 0;
    private static final int ROLE_ALTERNATE_SHOT = 1;
    private static final int ROLE_MODEL_PREFERRED = 2;

    // 관심도 점수 밴드 (기획서 Step 10-A 계열 중, 현재 로깅되는 이벤트로 매칭 가능한 항목만 반영).
    // size/stock check, reco_click, product_exit, 상세탭 명확화는 로깅 자체가 없어 이번 범위에서 제외한다.
    private static final int TAG_SCAN_POINTS = 5;
    private static final int REVISIT_POINTS = 10; // TAG_SCAN_POINTS에 가산 (동일 SKU 2회 이상 스캔 시 5+10=15)
    private static final int PURCHASE_INQUIRY_POINTS = 35;
    private static final int TRYON_REQUEST_POINTS = 30;
    private static final int CONTACT_SENT_POINTS = 20; // product 단위 1회만 (SKU별로 중복 가산하지 않음)
    private static final int PRICE_INQUIRY_POINTS = 15;
    private static final int SUBHUB_CLICK_POINTS = 3; // InteractionLog.subOption != null 건당
    private static final int HUB_CLICK_POINTS = 2; // InteractionLog.subOption == null 건당
    private static final String PRICE_SUB_OPTION = "PRICE";

    private final SessionRepository sessionRepository;
    private final TagScanLogRepository tagScanLogRepository;
    private final InteractionLogRepository interactionLogRepository;
    private final PurchaseInquiryRepository purchaseInquiryRepository;
    private final TryonRequestRepository tryonRequestRepository;
    private final ContactRepository contactRepository;
    private final SkuImageRepository skuImageRepository;

    public JourneyCardResponse getJourneyCard(String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        List<TagScanLog> scanLogs = tagScanLogRepository.findBySession_SessionIdOrderByScanOrderAsc(sessionId);
        List<InteractionLog> interactionLogs = interactionLogRepository.findBySession_SessionId(sessionId);
        List<PurchaseInquiry> purchaseInquiries = purchaseInquiryRepository.findBySession_SessionId(sessionId);
        List<TryonRequest> tryonRequests = tryonRequestRepository.findBySession_SessionId(sessionId);
        List<Contact> contacts = contactRepository.findBySession_SessionId(sessionId);

        List<CollageImageResponse> collageImages = buildCollageImages(
                sessionId, scanLogs, interactionLogs, purchaseInquiries, tryonRequests, contacts);

        return new JourneyCardResponse(
                BRAND,
                session.getCreatedAt().format(DATE_FORMATTER),
                session.getNickname(),
                sessionId.substring(0, SESSION_CODE_LENGTH),
                collageImages,
                collageImages.size() == TARGET_TOTAL_COUNT
        );
    }

    private List<CollageImageResponse> buildCollageImages(
            String sessionId,
            List<TagScanLog> scanLogs,
            List<InteractionLog> interactionLogs,
            List<PurchaseInquiry> purchaseInquiries,
            List<TryonRequest> tryonRequests,
            List<Contact> contacts
    ) {
        // style_number(=Sku, 1:1)별 "처음 태그된" Sku. LinkedHashMap이라 태그 순서가 그대로 유지되고,
        // 이 순서가 동점일 때의 타이브레이커(먼저 태그한 순서)로도 쓰인다.
        Map<String, Sku> firstTaggedSkuByStyle = new LinkedHashMap<>();
        for (TagScanLog scanLog : scanLogs) {
            Sku sku = scanLog.getSku();
            if (sku == null || sku.getStyleNumber() == null) {
                continue;
            }
            firstTaggedSkuByStyle.putIfAbsent(sku.getStyleNumber(), sku);
        }
        if (firstTaggedSkuByStyle.isEmpty()) {
            return List.of();
        }

        List<String> taggedStyleNumbers = List.copyOf(firstTaggedSkuByStyle.keySet());
        Map<String, Integer> skuScores = calculateSkuInterestScores(
                sessionId, taggedStyleNumbers, scanLogs, interactionLogs, purchaseInquiries, tryonRequests);

        // SKU 점수를 상품(productId) 단위로 합산. representativeStyleByProduct는 그 상품이 세션에서
        // 처음 태그된 순서를 유지하므로, 상품 간 동점 타이브레이커(먼저 태그한 순서)로 그대로 쓸 수 있다.
        Map<Long, Integer> productScores = new LinkedHashMap<>();
        Map<Long, String> representativeStyleByProduct = new LinkedHashMap<>();
        for (String styleNumber : taggedStyleNumbers) {
            Long productId = firstTaggedSkuByStyle.get(styleNumber).getProduct().getProductId();
            productScores.merge(productId, skuScores.getOrDefault(styleNumber, 0), Integer::sum);
            representativeStyleByProduct.putIfAbsent(productId, styleNumber);
        }

        applyContactBonus(sessionId, contacts, productScores, representativeStyleByProduct.keySet());

        // 점수 내림차순. Stream.sorted는 안정 정렬이므로 동점이면 representativeStyleByProduct의 순서
        // (=상품이 세션에서 처음 태그된 순서)가 유지된다.
        List<Long> rankedProductIds = representativeStyleByProduct.keySet().stream()
                .sorted(Comparator.comparingInt((Long productId) -> productScores.getOrDefault(productId, 0))
                        .reversed())
                .toList();

        log.info("[JourneyCard] session={} 상품 순위 확정: {}", sessionId,
                rankedProductIds.stream()
                        .map(productId -> "product=%d(score=%d)"
                                .formatted(productId, productScores.getOrDefault(productId, 0)))
                        .toList());

        List<String> rankedStyleNumbers = rankedProductIds.stream()
                .map(representativeStyleByProduct::get)
                .toList();

        Map<String, List<SkuImage>> imagesByStyle = skuImageRepository.findByStyleNumberIn(rankedStyleNumbers).stream()
                .collect(Collectors.groupingBy(SkuImage::getStyleNumber));

        List<CollageImageResponse> collageImages = new ArrayList<>();
        for (int rank = 0; rank < rankedStyleNumbers.size() && collageImages.size() < TARGET_TOTAL_COUNT; rank++) {
            List<SkuImage> images = imagesByStyle.getOrDefault(rankedStyleNumbers.get(rank), List.of());
            SkuImage picked = pickImageForRole(rank, images);
            if (picked != null) {
                collageImages.add(new CollageImageResponse(picked.getImageUrl(), picked.getShotType()));
            }
        }
        return collageImages;
    }

    /**
     * SKU(=style_number) 단위 관심도 점수. 상품 단위 합산은 buildCollageImages에서 처리한다.
     * 각 항목의 점수 산출 근거를 INFO 로그로 남겨 나중에 디버깅할 수 있게 한다.
     */
    private Map<String, Integer> calculateSkuInterestScores(
            String sessionId,
            List<String> candidateStyleNumbers,
            List<TagScanLog> scanLogs,
            List<InteractionLog> interactionLogs,
            List<PurchaseInquiry> purchaseInquiries,
            List<TryonRequest> tryonRequests
    ) {
        Map<String, Long> scanCountByStyle = scanLogs.stream()
                .filter(scanLog -> scanLog.getSku() != null && scanLog.getSku().getStyleNumber() != null)
                .collect(Collectors.groupingBy(
                        scanLog -> scanLog.getSku().getStyleNumber(), Collectors.counting()));

        Set<String> stylesWithPurchaseInquiry = purchaseInquiries.stream()
                .map(JourneyCardService::styleNumberOfSku)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> stylesWithTryonRequest = tryonRequests.stream()
                .map(JourneyCardService::styleNumberOfSku)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, List<InteractionLog>> interactionsByStyle = interactionLogs.stream()
                .filter(interactionLog -> interactionLog.getSku() != null
                        && interactionLog.getSku().getStyleNumber() != null)
                .collect(Collectors.groupingBy(interactionLog -> interactionLog.getSku().getStyleNumber()));

        Map<String, Integer> scores = new LinkedHashMap<>();
        for (String styleNumber : candidateStyleNumbers) {
            long scanCount = scanCountByStyle.getOrDefault(styleNumber, 0L);
            int tagScanPoints = scanCount > 0 ? TAG_SCAN_POINTS : 0;
            int revisitPoints = scanCount >= 2 ? REVISIT_POINTS : 0;
            int purchaseInquiryPoints = stylesWithPurchaseInquiry.contains(styleNumber) ? PURCHASE_INQUIRY_POINTS : 0;
            int tryonRequestPoints = stylesWithTryonRequest.contains(styleNumber) ? TRYON_REQUEST_POINTS : 0;

            List<InteractionLog> logsForStyle = interactionsByStyle.getOrDefault(styleNumber, List.of());
            boolean hasPriceInquiry = logsForStyle.stream().anyMatch(entry ->
                    entry.getInterestType() == InterestType.PURCHASE_CONDITION
                            && PRICE_SUB_OPTION.equals(entry.getSubOption()));
            int priceInquiryPoints = hasPriceInquiry ? PRICE_INQUIRY_POINTS : 0;

            long subhubClicks = logsForStyle.stream().filter(entry -> entry.getSubOption() != null).count();
            long hubClicks = logsForStyle.stream().filter(entry -> entry.getSubOption() == null).count();
            int subhubPoints = (int) (subhubClicks * SUBHUB_CLICK_POINTS);
            int hubPoints = (int) (hubClicks * HUB_CLICK_POINTS);

            int total = tagScanPoints + revisitPoints + purchaseInquiryPoints + tryonRequestPoints
                    + priceInquiryPoints + subhubPoints + hubPoints;
            scores.put(styleNumber, total);

            log.info(
                    "[JourneyCard] session={} styleNumber={} skuScore={} (tagScan={}, revisit={}, "
                            + "purchaseInquiry={}, tryonRequest={}, priceInquiry={}, "
                            + "subhubClicks={}x{}={}, hubClicks={}x{}={})",
                    sessionId, styleNumber, total, tagScanPoints, revisitPoints, purchaseInquiryPoints,
                    tryonRequestPoints, priceInquiryPoints,
                    subhubClicks, SUBHUB_CLICK_POINTS, subhubPoints, hubClicks, HUB_CLICK_POINTS, hubPoints
            );
        }
        return scores;
    }

    // Contact는 sku가 아닌 productId를 직접 들고 있으므로 SKU 점수와 별도로, 상품당 1회만 더한다.
    private void applyContactBonus(
            String sessionId, List<Contact> contacts, Map<Long, Integer> productScores,
            Set<Long> candidateProductIds
    ) {
        Set<Long> contactedProductIds = contacts.stream()
                .filter(Contact::isContentSent)
                .map(Contact::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Long productId : candidateProductIds) {
            if (contactedProductIds.contains(productId)) {
                productScores.merge(productId, CONTACT_SENT_POINTS, Integer::sum);
                log.info("[JourneyCard] session={} productId={} contactSent=+{}",
                        sessionId, productId, CONTACT_SENT_POINTS);
            }
        }
    }

    private static String styleNumberOfSku(PurchaseInquiry purchaseInquiry) {
        Sku sku = purchaseInquiry.getSku();
        return sku == null ? null : sku.getStyleNumber();
    }

    private static String styleNumberOfSku(TryonRequest tryonRequest) {
        Sku sku = tryonRequest.getSku();
        return sku == null ? null : sku.getStyleNumber();
    }

    private SkuImage pickImageForRole(int role, List<SkuImage> images) {
        return switch (role) {
            case ROLE_FRONT_SHOT -> pickFrontProductShot(images);
            case ROLE_ALTERNATE_SHOT -> pickAlternateProductShot(images);
            case ROLE_MODEL_PREFERRED -> pickModelPreferredShot(images);
            default -> pickConceptSlotImage(images);
        };
    }

    // 1위: 정면샷 개념 - PRODUCT 중 position이 가장 앞선 것. 없으면 MODEL로 대체.
    private SkuImage pickFrontProductShot(List<SkuImage> images) {
        List<SkuImage> productShots = sortByPosition(images, ShotType.PRODUCT);
        if (!productShots.isEmpty()) {
            return productShots.get(0);
        }
        List<SkuImage> modelShots = sortByPosition(images, ShotType.MODEL);
        return modelShots.isEmpty() ? null : modelShots.get(0);
    }

    // 2위: 다른 각도/흰배경샷 개념 - PRODUCT 중 position이 두 번째로 앞선 것. 한 장뿐이면 그 한 장, 없으면 MODEL로 대체.
    private SkuImage pickAlternateProductShot(List<SkuImage> images) {
        List<SkuImage> productShots = sortByPosition(images, ShotType.PRODUCT);
        if (productShots.size() >= 2) {
            return productShots.get(1);
        }
        if (!productShots.isEmpty()) {
            return productShots.get(0);
        }
        List<SkuImage> modelShots = sortByPosition(images, ShotType.MODEL);
        return modelShots.isEmpty() ? null : modelShots.get(0);
    }

    // 3위: 모델샷 우선, 없으면 PRODUCT로 대체.
    private SkuImage pickModelPreferredShot(List<SkuImage> images) {
        List<SkuImage> modelShots = sortByPosition(images, ShotType.MODEL);
        if (!modelShots.isEmpty()) {
            return modelShots.get(0);
        }
        List<SkuImage> productShots = sortByPosition(images, ShotType.PRODUCT);
        return productShots.isEmpty() ? null : productShots.get(0);
    }

    // 4번째 슬롯(컨셉샷): 전용 리소스가 생기기 전까지는 해당 순위 제품의 대표 이미지로 대체한다.
    // 실제 컨셉샷 리소스가 생기면 이 메서드만 고정 이미지 반환으로 교체하면 된다.
    private SkuImage pickConceptSlotImage(List<SkuImage> images) {
        return pickFrontProductShot(images);
    }

    private List<SkuImage> sortByPosition(List<SkuImage> images, ShotType shotType) {
        return images.stream()
                .filter(image -> image.getShotType() == shotType)
                .sorted(Comparator.comparing(SkuImage::getPosition))
                .toList();
    }
}
