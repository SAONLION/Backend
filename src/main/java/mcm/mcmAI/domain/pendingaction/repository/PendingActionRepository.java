package mcm.mcmAI.domain.pendingaction.repository;

import java.util.Optional;
import mcm.mcmAI.domain.pendingaction.entity.PendingAction;
import mcm.mcmAI.domain.pendingaction.type.BlockerType;
import mcm.mcmAI.domain.pendingaction.type.PendingActionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingActionRepository extends JpaRepository<PendingAction, Long> {

    Optional<PendingAction> findFirstBySession_SessionIdAndStatusOrderByCreatedAtDesc(
            String sessionId, PendingActionStatus status
    );

    boolean existsByStaffCall_CallIdAndBlockerType(Long callId, BlockerType blockerType);
}