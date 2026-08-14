package mcm.mcmAI.domain.tagscanlog.repository;

import mcm.mcmAI.domain.tagscanlog.entity.TagScanLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagScanLogRepository extends JpaRepository<TagScanLog, Long> {

    long countBySession_SessionId(String sessionId);
}