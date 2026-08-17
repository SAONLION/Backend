package mcm.mcmAI.domain.journeycard.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.journeycard.dto.CollageImageResponse;
import mcm.mcmAI.domain.journeycard.dto.JourneyCardResponse;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
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
    private static final int TARGET_MODEL_COUNT = 1;
    private static final int TARGET_TOTAL_COUNT = 4;

    private final SessionRepository sessionRepository;
    private final TagScanLogRepository tagScanLogRepository;
    private final SkuImageRepository skuImageRepository;

    public JourneyCardResponse getJourneyCard(String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        List<TagScanLog> scanLogs = tagScanLogRepository.findBySession_SessionIdOrderByScanOrderAsc(sessionId);

        return new JourneyCardResponse(
                BRAND,
                session.getCreatedAt().format(DATE_FORMATTER),
                session.getNickname(),
                sessionId.substring(0, SESSION_CODE_LENGTH),
                buildCollageImages(scanLogs)
        );
    }

    private List<CollageImageResponse> buildCollageImages(List<TagScanLog> scanLogs) {
        List<String> styleNumbers = scanLogs.stream()
                .map(scanLog -> scanLog.getSku().getStyleNumber())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (styleNumbers.isEmpty()) {
            return List.of();
        }

        Map<String, List<SkuImage>> imagesByStyle = skuImageRepository.findByStyleNumberIn(styleNumbers).stream()
                .collect(Collectors.groupingBy(SkuImage::getStyleNumber));

        List<SkuImage> modelPicks = pickRoundRobin(styleNumbers, imagesByStyle, ShotType.MODEL, TARGET_MODEL_COUNT);
        int remainingSlots = TARGET_TOTAL_COUNT - modelPicks.size();
        List<SkuImage> productPicks = pickRoundRobin(styleNumbers, imagesByStyle, ShotType.PRODUCT, remainingSlots);

        return Stream.concat(modelPicks.stream(), productPicks.stream())
                .map(image -> new CollageImageResponse(image.getImageUrl(), image.getShotType()))
                .toList();
    }

    // 한 제품에서만 몰아 뽑지 않도록 태그한 제품들을 순서대로 한 장씩 돌아가며 채운다.
    private List<SkuImage> pickRoundRobin(
            List<String> styleNumbers, Map<String, List<SkuImage>> imagesByStyle, ShotType shotType, int limit
    ) {
        if (limit <= 0) {
            return List.of();
        }

        Map<String, Iterator<SkuImage>> iteratorsByStyle = new LinkedHashMap<>();
        for (String styleNumber : styleNumbers) {
            List<SkuImage> images = imagesByStyle.getOrDefault(styleNumber, List.of()).stream()
                    .filter(image -> image.getShotType() == shotType)
                    .sorted(Comparator.comparing(SkuImage::getPosition))
                    .toList();
            iteratorsByStyle.put(styleNumber, images.iterator());
        }

        List<SkuImage> picked = new ArrayList<>();
        boolean progressed = true;
        while (picked.size() < limit && progressed) {
            progressed = false;
            for (Iterator<SkuImage> iterator : iteratorsByStyle.values()) {
                if (picked.size() >= limit) {
                    break;
                }
                if (iterator.hasNext()) {
                    picked.add(iterator.next());
                    progressed = true;
                }
            }
        }
        return picked;
    }
}