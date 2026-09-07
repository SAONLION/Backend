package mcm.mcmAI.domain.staffcall.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import mcm.mcmAI.domain.staffcall.dto.StaffCallBoardItem;
import mcm.mcmAI.domain.staffcall.dto.StaffCallBoardResponse;
import mcm.mcmAI.domain.staffcall.dto.StaffCallRequest;
import mcm.mcmAI.domain.staffcall.dto.StaffCallResponse;
import mcm.mcmAI.domain.staffcall.dto.StaffCallStatusResponse;
import mcm.mcmAI.domain.staffcall.entity.StaffCall;
import mcm.mcmAI.domain.staffcall.repository.StaffCallRepository;
import mcm.mcmAI.domain.staffcall.type.StaffCallStatus;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffCallService {

    private final StaffCallRepository staffCallRepository;
    private final SessionRepository sessionRepository;
    private final SkuRepository skuRepository;

    @Transactional
    public StaffCallResponse createStaffCall(String sessionId, StaffCallRequest request) {
        Session session = findSession(sessionId);

        Sku sku = skuRepository.findBySkuAndIsDeletedFalse(request.sku())
                .orElseThrow(() -> new BusinessException(ErrorCode.SKU_NOT_FOUND));

        StaffCall staffCall = StaffCall.builder()
                .session(session)
                .sku(sku)
                .reason(request.reason())
                .build();

        return StaffCallResponse.from(staffCallRepository.save(staffCall));
    }

    public StaffCallStatusResponse getStaffCall(String sessionId, Long callId) {
        StaffCall staffCall = staffCallRepository.findByCallIdAndSession_SessionId(callId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_NOT_FOUND));

        return StaffCallStatusResponse.from(staffCall);
    }

    public StaffCallBoardResponse getBoard(int completedLimit) {
        List<StaffCallBoardItem> waiting = staffCallRepository
                .findByStatusNotOrderByRequestedAtAsc(StaffCallStatus.COMPLETED).stream()
                .map(StaffCallBoardItem::from)
                .toList();

        List<StaffCallBoardItem> completed = staffCallRepository
                .findByStatusOrderByUpdatedAtDesc(StaffCallStatus.COMPLETED, PageRequest.of(0, completedLimit)).stream()
                .map(StaffCallBoardItem::from)
                .toList();

        return new StaffCallBoardResponse(waiting, completed);
    }

    @Transactional
    public StaffCallStatusResponse completeStaffCall(Long callId) {
        StaffCall staffCall = staffCallRepository.findById(callId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_NOT_FOUND));

        staffCall.changeStatus(StaffCallStatus.COMPLETED);

        return StaffCallStatusResponse.from(staffCall);
    }

    @Transactional
    public StaffCallResponse changeStatusForTest(Long callId, String statusValue) {
        StaffCall staffCall = staffCallRepository.findById(callId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_NOT_FOUND));

        staffCall.changeStatus(StaffCallStatus.from(statusValue));

        return StaffCallResponse.from(staffCall);
    }

    @Transactional
    public StaffCallResponse changeRequestedAtForTest(Long callId, String sessionId, LocalDateTime requestedAt) {
        StaffCall staffCall = staffCallRepository.findByCallIdAndSession_SessionId(callId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_NOT_FOUND));

        staffCall.changeRequestedAt(requestedAt);

        return StaffCallResponse.from(staffCall);
    }

    private Session findSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }
}
