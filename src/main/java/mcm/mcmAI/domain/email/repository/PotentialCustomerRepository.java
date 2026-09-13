package mcm.mcmAI.domain.email.repository;

import java.util.List;
import mcm.mcmAI.domain.email.entity.PotentialCustomer;
import mcm.mcmAI.domain.email.type.SentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PotentialCustomerRepository extends JpaRepository<PotentialCustomer, Long> {

    List<PotentialCustomer> findBySession_SessionIdOrderByPcIdDesc(String sessionId);

    /** 실패(FAILED)는 실제로 나간 게 아니므로 세션당 발송 횟수 한도에서 제외하고 센다. */
    long countBySession_SessionIdAndSentStatusNot(String sessionId, SentStatus sentStatus);
}
