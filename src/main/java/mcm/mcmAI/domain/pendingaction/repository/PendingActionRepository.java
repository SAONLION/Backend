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

    boolean existsByTriggerTagScanLog_ScanIdAndBlockerType(Long scanId, BlockerType blockerType);

    boolean existsByTriggerInteractionLog_InteractionIdAndBlockerType(Long interactionId, BlockerType blockerType);

    boolean existsBySession_SessionIdAndProduct_ProductIdAndBlockerTypeAndStatus(
            String sessionId, Long productId, BlockerType blockerType, PendingActionStatus status);
}