package mcm.mcmAI.domain.sku.repository;

import java.util.List;
import java.util.Optional;
import mcm.mcmAI.domain.sku.entity.Sku;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuRepository extends JpaRepository<Sku, Long> {

    List<Sku> findByProduct_ProductIdAndIsDeletedFalseOrderBySkuAsc(Long productId);

    Optional<Sku> findBySkuAndIsDeletedFalse(Long sku);
}