package mcm.mcmAI.domain.tryonrequest.repository;

import mcm.mcmAI.domain.tryonrequest.entity.TryonRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TryonRequestRepository extends JpaRepository<TryonRequest, Long> {
    List<TryonRequest> findBySession_SessionId(String sessionId);

    Optional<TryonRequest> findByTryonRequestIdAndSession_SessionId(Long tryonRequestId, String sessionId);
}
