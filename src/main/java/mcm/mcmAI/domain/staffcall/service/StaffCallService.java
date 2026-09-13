package mcm.mcmAI.domain.staffcall.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.pendingaction.entity.PendingAction;
import mcm.mcmAI.domain.purchaseinquiry.entity.PurchaseInquiry;
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
import mcm.mcmAI.domain.tryonrequest.entity.TryonRequest;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffCallService {

    private static final String TRYON_REQUEST_REASON = "착용 요청";
    private static final String PURCHASE_INQUIRY_REASON = "구매 문의";

    private final StaffCallRepository staffCallRepository;
    private final SessionRepository sessionRepository;
    private final SkuRepository skuRepository;

    @Transactional
    public StaffCallResponse createStaffCall(String sessionId, StaffCallRequest request) {
        Session session = findSession(sessionId);

        Sku sku = null;
        if (request.sku() != null) {
            sku = skuRepository.findBySkuAndIsDeletedFalse(request.sku())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SKU_NOT_FOUND));
        }

        StaffCall staffCall = StaffCall.builder()
                .session(session)
                .sku(sku)
                .reason(request.reason())
                .build();

        return StaffCallResponse.from(staffCallRepository.save(staffCall));
    }

    @Transactional
    public void createForTryonRequest(TryonRequest tryonRequest) {
        if (staffCallRepository.existsByTryonRequest_TryonRequestId(tryonRequest.getTryonRequestId())) {
            return;
        }

        staffCallRepository.save(StaffCall.builder()
                .session(tryonRequest.getSession())
                .sku(tryonRequest.getSku())
                .reason(TRYON_REQUEST_REASON)
                .size(tryonRequest.getSize())
                .tryonRequest(tryonRequest)
                .build());
    }

    @Transactional
    public void createForPurchaseInquiry(PurchaseInquiry purchaseInquiry) {
        if (staffCallRepository.existsByPurchaseInquiry_PurchaseInquiryId(purchaseInquiry.getPurchaseInquiryId())) {
            return;
        }

        staffCallRepository.save(StaffCall.builder()
                .session(purchaseInquiry.getSession())
                .sku(purchaseInquiry.getSku())
                .reason(PURCHASE_INQUIRY_REASON)
                .purchaseInquiry(purchaseInquiry)
                .build());
    }

    @Transactional
    public void createForPendingAction(PendingAction pendingAction, String reason) {
        if (staffCallRepository.existsByPendingAction_ActionId(pendingAction.getActionId())) {
            return;
        }

        staffCallRepository.save(StaffCall.builder()
                .session(pendingAction.getSession())
                .sku(pendingAction.getSku())
                .reason(reason)
                .pendingAction(pendingAction)
                .build());
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
