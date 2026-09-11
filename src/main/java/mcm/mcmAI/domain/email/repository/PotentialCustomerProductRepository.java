package mcm.mcmAI.domain.email.repository;

import java.util.List;
import mcm.mcmAI.domain.email.entity.PotentialCustomerProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PotentialCustomerProductRepository extends JpaRepository<PotentialCustomerProduct, Long> {

    List<PotentialCustomerProduct> findByPotentialCustomer_PcIdOrderBySlotTypeAscSlotOrderAsc(Long pcId);
}
