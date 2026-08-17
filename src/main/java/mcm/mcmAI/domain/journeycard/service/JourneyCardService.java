package mcm.mcmAI.domain.journeycard.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.interactionlog.entity.InteractionLog;
import mcm.mcmAI.domain.interactionlog.repository.InteractionLogRepository;
import mcm.mcmAI.domain.journeycard.dto.CollageImageResponse;
import mcm.mcmAI.domain.journeycard.dto.JourneyCardResponse;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.skuimage.entity.SkuImage;
import mcm.mcmAI.domain.skuimage.repository.SkuImageRepository;
import mcm.mcmAI.domain.skuimage.type.ShotType;
import mcm.mcmAI.domain.tagscanlog.entity.TagScanLog;
import mcm.mcmAI.domain.tagscanlog.repository.TagScanLogRepository;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final SessionRepository sessionRepository;
    private final TagScanLogRepository tagScanLogRepository;
    private final InteractionLogRepository interactionLogRepository;
    private final SkuImageRepository skuImageRepository;

    public JourneyCardResponse getJourneyCard(String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        List<TagScanLog> scanLogs = tagScanLogRepository.findBySession_SessionIdOrderByScanOrderAsc(sessionId);
        List<InteractionLog> interactionLogs = interactionLogRepository.findBySession_SessionId(sessionId);

        List<CollageImageResponse> collageImages = buildCollageImages(scanLogs, interactionLogs);

        return new JourneyCardResponse(
                BRAND,
                session.getCreatedAt().format(DATE_FORMATTER),
                session.getNickname(),
                sessionId.substring(0, SESSION_CODE_LENGTH),
                collageImages,
                collageImages.size() == TARGET_TOTAL_COUNT
        );
    }

    private List<CollageImageResponse> buildCollageImages(List<TagScanLog> scanLogs, List<InteractionLog> interactionLogs) {
        List<String> taggedStyleNumbers = scanLogs.stream()
                .map(scanLog -> scanLog.getSku().getStyleNumber())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (taggedStyleNumbers.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> interestScores = calculateInterestScores(taggedStyleNumbers, interactionLogs);

        // 점수 내림차순. Stream.sorted는 안정 정렬이므로 동점이면 태그 순서(taggedStyleNumbers 원래 순서)가 유지된다.
        List<String> rankedStyleNumbers = taggedStyleNumbers.stream()
                .sorted(Comparator.comparingInt((String styleNumber) -> interestScores.getOrDefault(styleNumber, 0))
                        .reversed())
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
     * 세션 내 태그한 제품(style_number)별 관심도 점수를 계산한다.
     * 점수 = 해당 제품에 대한 interaction 호출 횟수 + durationSeconds 합계(null은 0으로 취급).
     * 상대적 순위만 정확하면 되므로 가중치는 대략적으로 1:1로 합산한다.
     */
    Map<String, Integer> calculateInterestScores(List<String> candidateStyleNumbers, List<InteractionLog> interactionLogs) {
        Map<String, Integer> scores = new HashMap<>();
        for (String styleNumber : candidateStyleNumbers) {
            scores.put(styleNumber, 0);
        }

        for (InteractionLog interactionLog : interactionLogs) {
            Sku sku = interactionLog.getSku();
            if (sku == null) {
                continue;
            }
            String styleNumber = sku.getStyleNumber();
            if (styleNumber == null || !scores.containsKey(styleNumber)) {
                continue;
            }
            int duration = interactionLog.getDurationSeconds() == null ? 0 : interactionLog.getDurationSeconds();
            scores.merge(styleNumber, 1 + duration, Integer::sum);
        }
        return scores;
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
