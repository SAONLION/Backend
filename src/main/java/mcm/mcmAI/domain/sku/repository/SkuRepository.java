package mcm.mcmAI.domain.sku.repository;

import java.util.List;
import mcm.mcmAI.domain.sku.entity.Sku;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuRepository extends JpaRepository<Sku, Long> {

    List<Sku> findByProduct_ProductIdOrderBySkuAsc(Long productId);
}