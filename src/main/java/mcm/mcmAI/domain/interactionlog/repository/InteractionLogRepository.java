package mcm.mcmAI.domain.interactionlog.repository;

import java.util.List;
import mcm.mcmAI.domain.interactionlog.entity.InteractionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionLogRepository extends JpaRepository<InteractionLog, Long> {

    List<InteractionLog> findBySession_SessionId(String sessionId);
}