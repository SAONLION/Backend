package mcm.mcmAI.domain.session.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.session.dto.NicknameUpdateRequest;
import mcm.mcmAI.domain.session.dto.NicknameUpdateResponse;
import mcm.mcmAI.domain.session.dto.SessionCreateRequest;
import mcm.mcmAI.domain.session.dto.SessionCreateResponse;
import mcm.mcmAI.domain.session.dto.SessionEndResponse;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private final SessionRepository sessionRepository;

    @Transactional
    public SessionCreateResponse create(SessionCreateRequest request) {
        String language = request == null ? null : request.language();

        Session session = Session.builder()
                .sessionId(UUID.randomUUID().toString())
                .language(language)
                .build();

        return SessionCreateResponse.from(sessionRepository.save(session));
    }

    @Transactional
    public NicknameUpdateResponse updateNickname(String sessionId, NicknameUpdateRequest request) {
        Session session = findSession(sessionId);
        session.changeNickname(request.nickname());
        return NicknameUpdateResponse.from(session);
    }

    @Transactional
    public SessionEndResponse end(String sessionId) {
        Session session = findSession(sessionId);
        session.end();
        return SessionEndResponse.from(session);
    }

    private Session findSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다: " + sessionId));
    }
}