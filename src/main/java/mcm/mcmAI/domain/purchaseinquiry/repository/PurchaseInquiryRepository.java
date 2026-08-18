package mcm.mcmAI.domain.purchaseinquiry.repository;

import java.time.LocalDateTime;
import mcm.mcmAI.domain.purchaseinquiry.entity.PurchaseInquiry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseInquiryRepository extends JpaRepository<PurchaseInquiry, Long> {

    boolean existsBySession_SessionIdAndInquiredAtAfter(String sessionId, LocalDateTime after);
}
