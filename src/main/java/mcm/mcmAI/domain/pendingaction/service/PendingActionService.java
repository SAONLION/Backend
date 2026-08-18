package mcm.mcmAI.domain.pendingaction.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.staffcall.entity.StaffCall;
import mcm.mcmAI.domain.staffcall.repository.StaffCallRepository;
import mcm.mcmAI.domain.staffcall.type.StaffCallStatus;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final PendingActionRepository pendingActionRepository;
    private final SessionRepository sessionRepository;
    private final StaffCallRepository staffCallRepository;

    @Transactional
    public PendingActionResponse getPendingAction(String sessionId) {
        Session session = findSession(sessionId);

        checkAndCreateCb3Blocker(session);

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

            createBlocker(
                    session, BlockerType.CB3, staffCall.getProduct(), staffCall,
                    CB3_POPUP_TITLE, CB3_POPUP_BODY, CB3_OPTIONS
            );
        }
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
        PendingAction pendingAction = PendingAction.builder()
                .session(session)
                .blockerType(blockerType)
                .product(product)
                .staffCall(staffCall)
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