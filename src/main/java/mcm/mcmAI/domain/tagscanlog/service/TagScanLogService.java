package mcm.mcmAI.domain.tagscanlog.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.tagscanlog.entity.TagScanLog;
import mcm.mcmAI.domain.tagscanlog.repository.TagScanLogRepository;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagScanLogService {

    private final TagScanLogRepository tagScanLogRepository;
    private final SessionRepository sessionRepository;

    @Transactional
    public void recordScan(String sessionId, Sku sku) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        int scanOrder = (int) tagScanLogRepository.countBySession_SessionId(sessionId) + 1;

        TagScanLog scanLog = TagScanLog.builder()
                .session(session)
                .sku(sku)
                .scanOrder(scanOrder)
                .scannedAt(LocalDateTime.now())
                .build();

        tagScanLogRepository.save(scanLog);
    }
}