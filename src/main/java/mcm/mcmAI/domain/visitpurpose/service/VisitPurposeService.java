package mcm.mcmAI.domain.visitpurpose.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.visitpurpose.dto.VisitPurposeRequest;
import mcm.mcmAI.domain.visitpurpose.dto.VisitPurposeResponse;
import mcm.mcmAI.domain.visitpurpose.dto.VisitPurposeStatusResponse;
import mcm.mcmAI.domain.visitpurpose.entity.VisitPurpose;
import mcm.mcmAI.domain.visitpurpose.repository.VisitPurposeRepository;
import mcm.mcmAI.domain.visitpurpose.type.PurposeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisitPurposeService {

    private final VisitPurposeRepository visitPurposeRepository;
    private final SessionRepository sessionRepository;

    public VisitPurposeStatusResponse get(String sessionId) {
        findSession(sessionId);

        return visitPurposeRepository.findBySession_SessionId(sessionId)
                .map(VisitPurposeStatusResponse::answered)
                .orElseGet(VisitPurposeStatusResponse::notAnswered);
    }

    @Transactional
    public VisitPurposeResponse save(String sessionId, VisitPurposeRequest request) {
        Session session = findSession(sessionId);

        VisitPurpose existing = visitPurposeRepository.findBySession_SessionId(sessionId)
                .orElse(null);
        if (existing != null) {
            return VisitPurposeResponse.from(existing);
        }

        PurposeType purposeType = PurposeType.from(request.purposeType());

        VisitPurpose visitPurpose = VisitPurpose.builder()
                .session(session)
                .purposeType(purposeType)
                .confirmedAt(LocalDateTime.now())
                .build();

        return VisitPurposeResponse.from(visitPurposeRepository.save(visitPurpose));
    }

    private Session findSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }
}