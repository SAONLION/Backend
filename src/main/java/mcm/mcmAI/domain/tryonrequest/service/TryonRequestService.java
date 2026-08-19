package mcm.mcmAI.domain.tryonrequest.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import mcm.mcmAI.domain.tryonrequest.dto.TryonRequestRequest;
import mcm.mcmAI.domain.tryonrequest.dto.TryonRequestResponse;
import mcm.mcmAI.domain.tryonrequest.entity.TryonRequest;
import mcm.mcmAI.domain.tryonrequest.repository.TryonRequestRepository;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TryonRequestService {

    private final TryonRequestRepository tryonRequestRepository;
    private final SessionRepository sessionRepository;
    private final SkuRepository skuRepository;

    @Transactional
    public TryonRequestResponse createTryonRequest(String sessionId, TryonRequestRequest request) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        Sku sku = skuRepository.findById(request.sku())
                .orElseThrow(() -> new BusinessException(ErrorCode.SKU_NOT_FOUND));

        TryonRequest tryonRequest = TryonRequest.builder()
                .session(session)
                .sku(sku)
                .size(request.size())
                .color(request.color())
                .build();

        return TryonRequestResponse.from(tryonRequestRepository.save(tryonRequest));
    }

    @Transactional
    public TryonRequestResponse changeRequestedAtForTest(Long tryonRequestId, String sessionId, LocalDateTime requestedAt) {
        TryonRequest tryonRequest = tryonRequestRepository.findByTryonRequestIdAndSession_SessionId(tryonRequestId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRYON_REQUEST_NOT_FOUND));

        tryonRequest.changeRequestedAt(requestedAt);

        return TryonRequestResponse.from(tryonRequest);
    }
}
