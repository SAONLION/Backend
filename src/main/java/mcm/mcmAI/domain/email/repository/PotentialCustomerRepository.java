package mcm.mcmAI.domain.email.repository;

import java.util.List;
import java.util.Optional;
import mcm.mcmAI.domain.email.entity.PotentialCustomer;
import mcm.mcmAI.domain.email.type.SentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PotentialCustomerRepository extends JpaRepository<PotentialCustomer, Long> {

    List<PotentialCustomer> findBySession_SessionIdOrderByPcIdDesc(String sessionId);

    Optional<PotentialCustomer> findFirstBySession_SessionIdAndSentStatusOrderByPcIdDesc(
            String sessionId, SentStatus sentStatus);

    boolean existsBySession_SessionIdAndSentStatus(String sessionId, SentStatus sentStatus);
}
