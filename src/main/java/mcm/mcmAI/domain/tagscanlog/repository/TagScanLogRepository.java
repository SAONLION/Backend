package mcm.mcmAI.domain.tagscanlog.repository;

import java.util.List;
import mcm.mcmAI.domain.tagscanlog.entity.TagScanLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagScanLogRepository extends JpaRepository<TagScanLog, Long> {

    long countBySession_SessionId(String sessionId);

    List<TagScanLog> findBySession_SessionIdOrderByScanOrderDesc(String sessionId);

    List<TagScanLog> findBySession_SessionIdOrderByScanOrderAsc(String sessionId);
}