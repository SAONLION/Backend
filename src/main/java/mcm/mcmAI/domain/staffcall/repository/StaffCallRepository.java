package mcm.mcmAI.domain.staffcall.repository;

import java.util.List;
import java.util.Optional;
import mcm.mcmAI.domain.staffcall.entity.StaffCall;
import mcm.mcmAI.domain.staffcall.type.StaffCallStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffCallRepository extends JpaRepository<StaffCall, Long> {

    Optional<StaffCall> findByCallIdAndSession_SessionId(Long callId, String sessionId);

    List<StaffCall> findBySession_SessionId(String sessionId);

    List<StaffCall> findBySession_SessionIdAndStatus(String sessionId, StaffCallStatus status);

    List<StaffCall> findBySession_SessionIdAndStatusNot(String sessionId, StaffCallStatus status);
}