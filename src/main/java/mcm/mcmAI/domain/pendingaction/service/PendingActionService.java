package mcm.mcmAI.domain.pendingaction.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.interactionlog.entity.InteractionLog;
import mcm.mcmAI.domain.interactionlog.repository.InteractionLogRepository;
import mcm.mcmAI.domain.pendingaction.dto.PendingActionResponse;
import mcm.mcmAI.domain.pendingaction.dto.RespondRequest;
import mcm.mcmAI.domain.pendingaction.dto.RespondResponse;
import mcm.mcmAI.domain.pendingaction.dto.StockCheckResultDTO;
import mcm.mcmAI.domain.pendingaction.entity.PendingAction;
import mcm.mcmAI.domain.pendingaction.entity.PendingActionOption;
import mcm.mcmAI.domain.pendingaction.repository.PendingActionRepository;
import mcm.mcmAI.domain.pendingaction.type.ActionNextStep;
import mcm.mcmAI.domain.pendingaction.type.BlockerType;
import mcm.mcmAI.domain.pendingaction.type.PendingActionStatus;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.type.InterestType;
import mcm.mcmAI.domain.purchaseinquiry.repository.PurchaseInquiryRepository;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.staffcall.entity.StaffCall;
import mcm.mcmAI.domain.staffcall.repository.StaffCallRepository;
import mcm.mcmAI.domain.staffcall.type.StaffCallStatus;
import mcm.mcmAI.domain.tagscanlog.entity.TagScanLog;
import mcm.mcmAI.domain.tagscanlog.repository.TagScanLogRepository;
import mcm.mcmAI.domain.tryonrequest.entity.TryonRequest;
import mcm.mcmAI.domain.tryonrequest.repository.TryonRequestRepository;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PendingActionService {

    private static final String DISMISSED_RESPONSE_KEY = "dismissed";

    private static final long CB3_UNANSWERED_THRESHOLD_MINUTES = 5;
    private static final String CB3_POPUP_TITLE = "고객님, 잠시만 기다려주세요";
    private static final String CB3_POPUP_BODY = "담당 직원을 우선적으로 호출해드릴까요?";
    private static final List<PendingActionOption> CB3_OPTIONS = List.of(
            new PendingActionOption("escalate_call", "우선 호출 요청", ActionNextStep.STAFF_CALL_CREATED)
    );
    private static final String TRIGGER_ID_CB3_1 = "T-CB3-1";
    private static final String PRICE_SUB_OPTION = "PRICE";

    private static final String TRIGGER_ID_CB5_1 = "T-CB5-1";
    // 동일 카테고리 내에서 "하위 가격대"로 간주하는 최소 하락폭. 코드에 공식 가격 티어 개념이 없어
    // RecommendationService의 ±30% 유사가격대 규칙을 참고해 20% 이상 하락을 임계값으로 삼는다.
    private static final double CB5_1_PRICE_DROP_RATIO = 0.2;
    private static final String CB5_1_POPUP_TITLE = "가격 부담이 있으실까요?";
    private static final String CB5_1_POPUP_BODY = "조금 더 합리적인 가격대의 제품도 함께 안내해드릴까요?";
    private static final List<PendingActionOption> CB5_1_OPTIONS = List.of(
            new PendingActionOption("recommend_alt", "합리적인 가격대 제품 추천받기", ActionNextStep.SHOW_RECOMMENDATIONS),
            new PendingActionOption("ask_staff", "직원에게 상담받기", ActionNextStep.STAFF_CALL_CREATED)
    );

    private static final String TRIGGER_ID_CB5_2 = "T-CB5-2";
    private static final long CB5_2_IDLE_THRESHOLD_MINUTES = 10;
    private static final List<String> CB5_2_PROMOTION_KEYWORDS = List.of("프로모션", "할인");
    private static final String CB5_2_POPUP_TITLE = "더 궁금하신 점 있으신가요?";
    private static final String CB5_2_POPUP_BODY = "가격 안내 이후 새로운 소식이 없으신 것 같아요. 필요하실 때 언제든 말씀해주세요.";
    private static final List<PendingActionOption> CB5_2_OPTIONS = List.of(
            new PendingActionOption("ask_staff", "직원에게 문의하기", ActionNextStep.STAFF_CALL_CREATED),
            new PendingActionOption("recommend_alt", "다른 상품 보기", ActionNextStep.SHOW_RECOMMENDATIONS)
    );

    private static final String TRIGGER_ID_CB6_A = "T-CB6-a";
    private static final long CB6_A_TRYON_ELAPSED_THRESHOLD_MINUTES = 15;
    private static final String CB6_A_POPUP_TITLE = "고민이 되실 만한 지점이 있으실까요?";
    private static final String CB6_A_POPUP_BODY = "착용해보신 제품, 결정에 도움이 될 정보를 더 안내해드릴까요?";
    private static final List<PendingActionOption> CB6_A_OPTIONS = List.of(
            new PendingActionOption("show_detail_reason", "결정 근거 더 보기", ActionNextStep.SHOW_RECOMMENDATIONS),
            new PendingActionOption("ask_staff", "직원과 상담하기", ActionNextStep.STAFF_CALL_CREATED)
    );

    private static final String TRIGGER_ID_CB6_B = "T-CB6-b";
    private static final int CB6_B_MIN_REVISIT_COUNT = 2;
    private static final long CB6_B_MIN_DWELL_MINUTES = 3;
    private static final String CB6_B_POPUP_TITLE = "이 제품, 계속 눈에 밟히시나요?";
    private static final String CB6_B_POPUP_BODY = "여러 번 확인하신 제품이에요. 결정에 도움이 필요하신가요?";
    private static final List<PendingActionOption> CB6_B_OPTIONS = List.of(
            new PendingActionOption("show_detail_reason", "결정 근거 더 보기", ActionNextStep.SHOW_RECOMMENDATIONS),
            new PendingActionOption("ask_staff", "직원과 상담하기", ActionNextStep.STAFF_CALL_CREATED)
    );

    private static final String TRIGGER_ID_CB6_C = "T-CB6-c";
    private static final long CB6_C_INACTIVITY_MINUTES = 5;
    private static final String CB6_C_POPUP_TITLE = "다른 제품도 보여드릴까요?";
    private static final String CB6_C_POPUP_BODY = "상담 후 결정이 어려우셨다면, 다른 옵션을 안내해드릴게요.";
    private static final List<PendingActionOption> CB6_C_OPTIONS = List.of(
            new PendingActionOption("recommend_alt", "다른 상품 추천받기", ActionNextStep.SHOW_RECOMMENDATIONS),
            new PendingActionOption("ask_staff", "직원과 다시 상담하기", ActionNextStep.STAFF_CALL_CREATED)
    );

    private final PendingActionRepository pendingActionRepository;
    private final SessionRepository sessionRepository;
    private final StaffCallRepository staffCallRepository;
    private final TagScanLogRepository tagScanLogRepository;
    private final InteractionLogRepository interactionLogRepository;
    private final PurchaseInquiryRepository purchaseInquiryRepository;
    private final TryonRequestRepository tryonRequestRepository;

    @Transactional
    public PendingActionResponse getPendingAction(String sessionId) {
        Session session = findSession(sessionId);

        checkAndCreateCb3Blocker(session);
        checkAndCreateCb5_1Blocker(session);
        checkAndCreateCb5_2Blocker(session);
        checkAndCreateCb6Blocker(session);

        return pendingActionRepository
                .findFirstBySession_SessionIdAndStatusOrderByCreatedAtDesc(sessionId, PendingActionStatus.PENDING)
                .map(PendingActionResponse::of)
                .orElseGet(PendingActionResponse::none);
    }

    @Transactional
    public void checkAndCreateCb3Blocker(Session session) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(CB3_UNANSWERED_THRESHOLD_MINUTES);

        List<StaffCall> unansweredCalls = staffCallRepository
                .findBySession_SessionIdAndStatus(session.getSessionId(), StaffCallStatus.REQUESTED);

        for (StaffCall staffCall : unansweredCalls) {
            LocalDateTime requestedAt = staffCall.getRequestedAt();
            if (requestedAt == null || requestedAt.isAfter(threshold)) {
                continue;
            }

            boolean alreadyCreated = pendingActionRepository
                    .existsByStaffCall_CallIdAndBlockerType(staffCall.getCallId(), BlockerType.CB3);
            if (alreadyCreated) {
                continue;
            }

            saveBlocker(
                    session, BlockerType.CB3, staffCall.getProduct(), staffCall, null, null, TRIGGER_ID_CB3_1,
                    CB3_POPUP_TITLE, CB3_POPUP_BODY, CB3_OPTIONS
            );
        }
    }

    @Transactional
    public void checkAndCreateCb5_1Blocker(Session session) {
        String sessionId = session.getSessionId();

        List<InteractionLog> priceDisclosures = interactionLogRepository.findBySession_SessionId(sessionId).stream()
                .filter(this::isPriceDisclosure)
                .sorted(Comparator.comparing(InteractionLog::getCreatedAt))
                .toList();
        if (priceDisclosures.isEmpty()) {
            return;
        }

        List<TagScanLog> scans = tagScanLogRepository.findBySession_SessionIdOrderByScanOrderAsc(sessionId);

        for (TagScanLog scan : scans) {
            InteractionLog priceDisclosure = latestBefore(priceDisclosures, scan.getScannedAt());
            if (priceDisclosure == null || !isLowerTierSwitch(priceDisclosure.getSku(), scan.getSku())) {
                continue;
            }

            boolean purchaseInquiryAfterDisclosure = purchaseInquiryRepository
                    .existsBySession_SessionIdAndInquiredAtAfter(sessionId, priceDisclosure.getCreatedAt());
            if (purchaseInquiryAfterDisclosure) {
                continue;
            }

            boolean alreadyCreated = pendingActionRepository
                    .existsByTriggerTagScanLog_ScanIdAndBlockerType(scan.getScanId(), BlockerType.CB5);
            if (alreadyCreated) {
                continue;
            }

            createCb5Blocker(
                    session, scan.getSku().getProduct(), scan, TRIGGER_ID_CB5_1,
                    CB5_1_POPUP_TITLE, CB5_1_POPUP_BODY, CB5_1_OPTIONS
            );
        }
    }

    @Transactional
    public void checkAndCreateCb5_2Blocker(Session session) {
        String sessionId = session.getSessionId();

        List<InteractionLog> interactionLogs = interactionLogRepository.findBySession_SessionId(sessionId);
        InteractionLog latestPriceDisclosure = interactionLogs.stream()
                .filter(this::isPriceDisclosure)
                .max(Comparator.comparing(InteractionLog::getCreatedAt))
                .orElse(null);
        if (latestPriceDisclosure == null) {
            return;
        }

        LocalDateTime gateTime = latestPriceDisclosure.getCreatedAt();
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(CB5_2_IDLE_THRESHOLD_MINUTES);
        if (gateTime.isAfter(threshold)) {
            return;
        }

        boolean hasNewerInteraction = interactionLogs.stream()
                .anyMatch(log -> log.getCreatedAt() != null && log.getCreatedAt().isAfter(gateTime));
        boolean hasNewerScan = tagScanLogRepository.findBySession_SessionIdOrderByScanOrderAsc(sessionId).stream()
                .anyMatch(scan -> scan.getScannedAt() != null && scan.getScannedAt().isAfter(gateTime));
        if (hasNewerInteraction || hasNewerScan) {
            return;
        }

        if (hasActivePromotionStaffCall(sessionId)) {
            return;
        }

        boolean alreadyCreated = pendingActionRepository.existsByTriggerInteractionLog_InteractionIdAndBlockerType(
                latestPriceDisclosure.getInteractionId(), BlockerType.CB5);
        if (alreadyCreated) {
            return;
        }

        createCb5Blocker(
                session, latestPriceDisclosure.getSku().getProduct(), latestPriceDisclosure, TRIGGER_ID_CB5_2,
                CB5_2_POPUP_TITLE, CB5_2_POPUP_BODY, CB5_2_OPTIONS
        );
    }

    // ===== CB6-a 판정 로직 =====
    @Transactional
    public void checkAndCreateCb6Blocker(Session session) {
        String sessionId = session.getSessionId();
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(CB6_A_TRYON_ELAPSED_THRESHOLD_MINUTES);

        List<TryonRequest> tryonRequests = tryonRequestRepository.findBySession_SessionId(sessionId);

        for (TryonRequest tryon : tryonRequests) {
            LocalDateTime requestedAt = tryon.getRequestedAt();
            if (requestedAt == null || requestedAt.isAfter(threshold)) {
                continue;
            }

            Product product = tryon.getSku().getProduct();

            // 배타 라우팅: CB3/CB5 조건에 이미 걸리면 CB6 평가 안 함
            // TODO: CB1(품절 조회) 배타 조건 — 재고조회 엔티티 확인 후 추가
            if (isCb3Excluded(sessionId) || isCb5PriceTierExcluded(sessionId, tryon.getSku())) {
                continue;
            }

            boolean purchaseInquiryAfterTryon = purchaseInquiryRepository
                    .existsBySession_SessionIdAndInquiredAtAfter(sessionId, requestedAt);
            if (purchaseInquiryAfterTryon) {
                continue;
            }

            boolean alreadyCreated = pendingActionRepository
                    .existsBySession_SessionIdAndProduct_ProductIdAndBlockerTypeAndStatus(
                            sessionId, product.getProductId(), BlockerType.CB6, PendingActionStatus.PENDING);
            if (alreadyCreated) {
                continue;
            }

            saveBlocker(
                    session, BlockerType.CB6, product, null, null, null, TRIGGER_ID_CB6_A,
                    CB6_A_POPUP_TITLE, CB6_A_POPUP_BODY, CB6_A_OPTIONS
            );
        }
    }

    private boolean isCb3Excluded(String sessionId) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(CB3_UNANSWERED_THRESHOLD_MINUTES);
        return staffCallRepository.findBySession_SessionIdAndStatus(sessionId, StaffCallStatus.REQUESTED).stream()
                .anyMatch(call -> call.getRequestedAt() != null && call.getRequestedAt().isBefore(threshold));
    }

    private boolean isCb5PriceTierExcluded(String sessionId, Sku baselineSku) {
        List<InteractionLog> priceDisclosures = interactionLogRepository.findBySession_SessionId(sessionId).stream()
                .filter(this::isPriceDisclosure)
                .toList();
        if (priceDisclosures.isEmpty()) {
            return false;
        }

        List<TagScanLog> scans = tagScanLogRepository.findBySession_SessionIdOrderByScanOrderAsc(sessionId);
        return scans.stream().anyMatch(scan -> {
            InteractionLog priceDisclosure = latestBefore(
                    priceDisclosures.stream().sorted(Comparator.comparing(InteractionLog::getCreatedAt)).toList(),
                    scan.getScannedAt()
            );
            return priceDisclosure != null && isLowerTierSwitch(priceDisclosure.getSku(), scan.getSku());
        });
    }

    @Transactional
    public void checkAndCreateCb6bBlocker(Session session) {
        String sessionId = session.getSessionId();
        List<TagScanLog> allScans = tagScanLogRepository.findBySession_SessionIdOrderByScanOrderAsc(sessionId);

        Map<Long, List<TagScanLog>> scansBySku = allScans.stream()
                .filter(scan -> scan.getSku() != null && scan.getScannedAt() != null)
                .collect(Collectors.groupingBy(scan -> scan.getSku().getSku()));

        for (Map.Entry<Long, List<TagScanLog>> entry : scansBySku.entrySet()) {
            List<TagScanLog> scans = entry.getValue();
            if (scans.size() < CB6_B_MIN_REVISIT_COUNT) {
                continue;
            }

            LocalDateTime firstSeen = scans.stream().map(TagScanLog::getScannedAt).min(LocalDateTime::compareTo).orElseThrow();
            LocalDateTime lastSeen = scans.stream().map(TagScanLog::getScannedAt).max(LocalDateTime::compareTo).orElseThrow();
            long dwellMinutes = Duration.between(firstSeen, lastSeen).toMinutes();
            if (dwellMinutes < CB6_B_MIN_DWELL_MINUTES) {
                continue;
            }

            Sku sku = scans.get(0).getSku();
            Product product = sku.getProduct();

            boolean hasNewerTagElsewhere = allScans.stream()
                    .anyMatch(scan -> !scan.getSku().getSku().equals(sku.getSku())
                            && scan.getScannedAt() != null && scan.getScannedAt().isAfter(lastSeen));
            if (hasNewerTagElsewhere) {
                continue;
            }

            if (isCb3Excluded(sessionId) || isCb5PriceTierExcluded(sessionId, sku)) {
                continue;
            }

            boolean purchaseInquiryAfter = purchaseInquiryRepository
                    .existsBySession_SessionIdAndInquiredAtAfter(sessionId, firstSeen);
            if (purchaseInquiryAfter) {
                continue;
            }

            boolean alreadyCreated = pendingActionRepository
                    .existsBySession_SessionIdAndProduct_ProductIdAndBlockerTypeAndStatus(
                            sessionId, product.getProductId(), BlockerType.CB6, PendingActionStatus.PENDING);
            if (alreadyCreated) {
                continue;
            }

            saveBlocker(
                    session, BlockerType.CB6, product, null, null, null, TRIGGER_ID_CB6_B,
                    CB6_B_POPUP_TITLE, CB6_B_POPUP_BODY, CB6_B_OPTIONS
            );
        }
    }

    @Transactional
    public void checkAndCreateCb6cBlocker(Session session) {
        String sessionId = session.getSessionId();
        LocalDateTime inactivityThreshold = LocalDateTime.now().minusMinutes(CB6_C_INACTIVITY_MINUTES);

        List<StaffCall> completedCalls = staffCallRepository
                .findBySession_SessionIdAndStatus(sessionId, StaffCallStatus.COMPLETED);

        for (StaffCall call : completedCalls) {
            LocalDateTime consultationEndedAt = call.getUpdatedAt();
            if (consultationEndedAt == null || consultationEndedAt.isAfter(inactivityThreshold)) {
                continue;
            }

            Product product = call.getProduct();
            if (product == null) {
                continue;
            }

            boolean purchaseAfter = purchaseInquiryRepository
                    .existsBySession_SessionIdAndInquiredAtAfter(sessionId, consultationEndedAt);
            if (purchaseAfter) {
                continue;
            }

            boolean hasActivityAfter = tagScanLogRepository.findBySession_SessionIdOrderByScanOrderAsc(sessionId).stream()
                    .anyMatch(scan -> scan.getScannedAt() != null && scan.getScannedAt().isAfter(consultationEndedAt))
                    || interactionLogRepository.findBySession_SessionId(sessionId).stream()
                    .anyMatch(log -> log.getCreatedAt() != null && log.getCreatedAt().isAfter(consultationEndedAt));
            if (hasActivityAfter) {
                continue;
            }

            boolean alreadyCreated = pendingActionRepository
                    .existsBySession_SessionIdAndProduct_ProductIdAndBlockerTypeAndStatus(
                            sessionId, product.getProductId(), BlockerType.CB6, PendingActionStatus.PENDING);
            if (alreadyCreated) {
                continue;
            }

            saveBlocker(
                    session, BlockerType.CB6, product, null, null, null, TRIGGER_ID_CB6_C,
                    CB6_C_POPUP_TITLE, CB6_C_POPUP_BODY, CB6_C_OPTIONS
            );
        }
    }
    private InteractionLog latestBefore(List<InteractionLog> sortedAscendingByCreatedAt, LocalDateTime timestamp) {
        InteractionLog latest = null;
        for (InteractionLog candidate : sortedAscendingByCreatedAt) {
            if (candidate.getCreatedAt() == null || !candidate.getCreatedAt().isBefore(timestamp)) {
                break;
            }
            latest = candidate;
        }
        return latest;
    }

    private boolean isPriceDisclosure(InteractionLog interactionLog) {
        return interactionLog.getInterestType() == InterestType.PURCHASE_CONDITION
                && PRICE_SUB_OPTION.equals(interactionLog.getSubOption());
    }

    private boolean isLowerTierSwitch(Sku disclosedSku, Sku scannedSku) {
        if (disclosedSku == null || scannedSku == null || disclosedSku.getSku().equals(scannedSku.getSku())) {
            return false;
        }

        Product disclosedProduct = disclosedSku.getProduct();
        Product scannedProduct = scannedSku.getProduct();
        if (disclosedProduct == null || scannedProduct == null
                || !disclosedProduct.getCategory().equals(scannedProduct.getCategory())) {
            return false;
        }

        Integer disclosedPrice = disclosedSku.getPrice();
        Integer scannedPrice = scannedSku.getPrice();
        if (disclosedPrice == null || scannedPrice == null) {
            return false;
        }

        return scannedPrice <= disclosedPrice * (1 - CB5_1_PRICE_DROP_RATIO);
    }

    private boolean hasActivePromotionStaffCall(String sessionId) {
        return staffCallRepository.findBySession_SessionIdAndStatusNot(sessionId, StaffCallStatus.COMPLETED).stream()
                .anyMatch(call -> containsPromotionKeyword(call.getReason()));
    }

    private boolean containsPromotionKeyword(String reason) {
        return reason != null && CB5_2_PROMOTION_KEYWORDS.stream().anyMatch(reason::contains);
    }

    @Transactional
    public RespondResponse respond(Long actionId, RespondRequest request) {
        PendingAction pendingAction = pendingActionRepository.findById(actionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTION_NOT_FOUND));

        String responseKey = request.responseKey();
        ActionNextStep nextStep = resolveNextStep(pendingAction, responseKey);

        pendingAction.respond(responseKey);

        StockCheckResultDTO result = nextStep == ActionNextStep.STOCK_REQUEST_COMPLETED
                ? StockCheckResultDTO.dummy()
                : null;

        return RespondResponse.of(pendingAction.getActionId(), responseKey, nextStep, result);
    }

    private ActionNextStep resolveNextStep(PendingAction pendingAction, String responseKey) {
        if (DISMISSED_RESPONSE_KEY.equals(responseKey)) {
            return ActionNextStep.NONE;
        }

        return pendingAction.findOption(responseKey)
                .map(PendingActionOption::actionNextStep)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_RESPONSE_KEY));
    }

    @Transactional
    public PendingAction createBlocker(
            Session session, BlockerType blockerType, Product product,
            String popupTitle, String popupBody, List<PendingActionOption> options
    ) {
        return createBlocker(session, blockerType, product, null, popupTitle, popupBody, options);
    }

    @Transactional
    public PendingAction createBlocker(
            Session session, BlockerType blockerType, Product product, StaffCall staffCall,
            String popupTitle, String popupBody, List<PendingActionOption> options
    ) {
        return saveBlocker(
                session, blockerType, product, staffCall, null, null, null,
                popupTitle, popupBody, options
        );
    }

    @Transactional
    public PendingAction createCb5Blocker(
            Session session, Product product, TagScanLog triggerTagScanLog, String triggerId,
            String popupTitle, String popupBody, List<PendingActionOption> options
    ) {
        return saveBlocker(
                session, BlockerType.CB5, product, null, triggerTagScanLog, null, triggerId,
                popupTitle, popupBody, options
        );
    }

    @Transactional
    public PendingAction createCb5Blocker(
            Session session, Product product, InteractionLog triggerInteractionLog, String triggerId,
            String popupTitle, String popupBody, List<PendingActionOption> options
    ) {
        return saveBlocker(
                session, BlockerType.CB5, product, null, null, triggerInteractionLog, triggerId,
                popupTitle, popupBody, options
        );
    }

    private PendingAction saveBlocker(
            Session session, BlockerType blockerType, Product product, StaffCall staffCall,
            TagScanLog triggerTagScanLog, InteractionLog triggerInteractionLog, String triggerId,
            String popupTitle, String popupBody, List<PendingActionOption> options
    ) {
        PendingAction pendingAction = PendingAction.builder()
                .session(session)
                .blockerType(blockerType)
                .product(product)
                .staffCall(staffCall)
                .triggerTagScanLog(triggerTagScanLog)
                .triggerInteractionLog(triggerInteractionLog)
                .triggerId(triggerId)
                .popupTitle(popupTitle)
                .popupBody(popupBody)
                .options(options)
                .build();

        return pendingActionRepository.save(pendingAction);
    }

    private Session findSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }
}