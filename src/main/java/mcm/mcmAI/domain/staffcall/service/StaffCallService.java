package mcm.mcmAI.domain.staffcall.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.staffcall.dto.StaffCallRequest;
import mcm.mcmAI.domain.staffcall.dto.StaffCallResponse;
import mcm.mcmAI.domain.staffcall.dto.StaffCallStatusResponse;
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
public class StaffCallService {

    private final StaffCallRepository staffCallRepository;
    private final SessionRepository sessionRepository;
    private final ProductRepository productRepository;

    @Transactional
    public StaffCallResponse createStaffCall(String sessionId, StaffCallRequest request) {
        Session session = findSession(sessionId);

        Long productId = request.productId();
        Product product = (productId != null)
                ? productRepository.findById(productId)
                  .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND))
                : null;

        StaffCall staffCall = StaffCall.builder()
                .session(session)
                .product(product)
                .reason(request.reason())
                .build();

        return StaffCallResponse.from(staffCallRepository.save(staffCall));
    }

    public StaffCallStatusResponse getStaffCall(String sessionId, Long callId) {
        StaffCall staffCall = staffCallRepository.findByCallIdAndSession_SessionId(callId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_NOT_FOUND));

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