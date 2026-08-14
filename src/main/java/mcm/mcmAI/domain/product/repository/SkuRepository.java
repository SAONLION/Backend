package mcm.mcmAI.domain.product.repository;

import mcm.mcmAI.domain.product.entity.Sku;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuRepository extends JpaRepository<Sku, Long> {
}