package mcm.mcmAI.domain.visitpurpose.repository;

import java.util.Optional;
import mcm.mcmAI.domain.visitpurpose.entity.VisitPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitPurposeRepository extends JpaRepository<VisitPurpose, Long> {

    Optional<VisitPurpose> findBySession_SessionId(String sessionId);
}