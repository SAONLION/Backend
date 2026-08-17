package mcm.mcmAI.domain.skuimage.repository;

import java.util.List;
import mcm.mcmAI.domain.skuimage.entity.SkuImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuImageRepository extends JpaRepository<SkuImage, Long> {

    List<SkuImage> findByStyleNumberIn(List<String> styleNumbers);
}