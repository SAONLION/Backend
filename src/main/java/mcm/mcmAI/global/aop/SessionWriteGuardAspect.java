package mcm.mcmAI.global.aop;

import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

@Aspect
@Component
@RequiredArgsConstructor
public class SessionWriteGuardAspect {

    private final SessionRepository sessionRepository;

    @Before("@annotation(requiresActiveSession)")
    public void checkSessionActive(JoinPoint joinPoint, RequiresActiveSession requiresActiveSession) {
        String sessionId = extractSessionId(joinPoint, requiresActiveSession.sessionIdParam());

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("session not found"));

        if (session.getEndedAt() != null) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ENDED);
        }
    }

    private String extractSessionId(JoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(paramName)) {
                return (String) args[i];
            }
        }
        throw new IllegalStateException("parameter " + paramName + " not found");
    }
}