package mcm.mcmAI.domain.interactionlog.service;

import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.interactionlog.dto.InteractionLogRequest;
import mcm.mcmAI.domain.interactionlog.dto.InteractionLogResponse;
import mcm.mcmAI.domain.interactionlog.entity.InteractionLog;
import mcm.mcmAI.domain.interactionlog.repository.InteractionLogRepository;
import mcm.mcmAI.domain.product.type.InterestType;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InteractionLogService {

    private final InteractionLogRepository interactionLogRepository;
    private final SessionRepository sessionRepository;
    private final SkuRepository skuRepository;

    @Transactional
    public InteractionLogResponse record(String sessionId, InteractionLogRequest request) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다: " + sessionId));

        Sku sku = skuRepository.findById(request.sku())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "SKU를 찾을 수 없습니다: " + request.sku()));

        InterestType interestType = InterestType.from(request.interestType());

        InteractionLog interactionLog = InteractionLog.builder()
                .session(session)
                .sku(sku)
                .interestType(interestType)
                .subOption(request.subOption())
                .build();

        return InteractionLogResponse.from(interactionLogRepository.save(interactionLog));
    }
}