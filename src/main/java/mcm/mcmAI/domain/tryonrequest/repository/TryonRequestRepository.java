package mcm.mcmAI.domain.tryonrequest.repository;

import mcm.mcmAI.domain.tryonrequest.entity.TryonRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TryonRequestRepository extends JpaRepository<TryonRequest, Long> {
}
