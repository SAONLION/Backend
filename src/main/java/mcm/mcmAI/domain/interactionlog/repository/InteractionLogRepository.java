package mcm.mcmAI.domain.interactionlog.repository;

import java.time.LocalDateTime;
import java.util.List;
import mcm.mcmAI.domain.interactionlog.entity.InteractionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InteractionLogRepository extends JpaRepository<InteractionLog, Long> {

    List<InteractionLog> findBySession_SessionId(String sessionId);

    // createdAt은 @CreatedDate(updatable=false)라 일반 저장으로는 과거 시각을 지정할 수 없다.
    // CB5-2(가격 공개 후 10분 무이벤트) 타임아웃 시나리오를 재현하는 테스트 전용 헬퍼.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE InteractionLog i SET i.createdAt = :createdAt WHERE i.interactionId = :interactionId")
    void forceCreatedAtForTest(@Param("interactionId") Long interactionId, @Param("createdAt") LocalDateTime createdAt);
}