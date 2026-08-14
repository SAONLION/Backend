package mcm.mcmAI.domain.product.repository;

import java.util.Optional;
import mcm.mcmAI.domain.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    Optional<ProductImage> findFirstBySkuOrderBySortOrderAsc(Long sku);
}