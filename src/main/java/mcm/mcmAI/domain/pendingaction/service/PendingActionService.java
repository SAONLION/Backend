package mcm.mcmAI.domain.pendingaction.service;

import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.pendingaction.dto.PendingActionResponse;
import mcm.mcmAI.domain.pendingaction.dto.RespondRequest;
import mcm.mcmAI.domain.pendingaction.dto.RespondResponse;
import mcm.mcmAI.domain.pendingaction.dto.StockCheckResultDTO;
import mcm.mcmAI.domain.pendingaction.entity.PendingAction;
import mcm.mcmAI.domain.pendingaction.entity.PendingActionOption;
import mcm.mcmAI.domain.pendingaction.repository.PendingActionRepository;
import mcm.mcmAI.domain.pendingaction.type.ActionNextStep;
import mcm.mcmAI.domain.pendingaction.type.PendingActionStatus;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PendingActionService {

    private static final String DISMISSED_RESPONSE_KEY = "dismissed";

    private final PendingActionRepository pendingActionRepository;
    private final SessionRepository sessionRepository;

    public PendingActionResponse getPendingAction(String sessionId) {
        findSession(sessionId);

        return pendingActionRepository
                .findFirstBySession_SessionIdAndStatusOrderByCreatedAtDesc(sessionId, PendingActionStatus.PENDING)
                .map(PendingActionResponse::of)
                .orElseGet(PendingActionResponse::none);
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

    private Session findSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }
}