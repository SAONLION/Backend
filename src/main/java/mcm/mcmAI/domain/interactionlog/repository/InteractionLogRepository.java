package mcm.mcmAI.domain.interactionlog.repository;

import mcm.mcmAI.domain.interactionlog.entity.InteractionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionLogRepository extends JpaRepository<InteractionLog, Long> {
}