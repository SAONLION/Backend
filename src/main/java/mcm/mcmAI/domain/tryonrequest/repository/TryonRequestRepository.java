package mcm.mcmAI.domain.tryonrequest.repository;

import mcm.mcmAI.domain.tryonrequest.entity.TryonRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TryonRequestRepository extends JpaRepository<TryonRequest, Long> {
    List<TryonRequest> findBySession_SessionId(String sessionId);
}
