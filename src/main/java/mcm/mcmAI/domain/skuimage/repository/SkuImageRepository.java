package mcm.mcmAI.domain.skuimage.repository;

import java.util.List;
import java.util.Optional;

import mcm.mcmAI.domain.skuimage.entity.SkuImage;
import mcm.mcmAI.domain.skuimage.type.ShotType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuImageRepository extends JpaRepository<SkuImage, Long> {

    List<SkuImage> findByStyleNumberInAndIsDeletedFalse(List<String> styleNumbers);

    List<SkuImage> findByStyleNumberAndIsDeletedFalseOrderByPositionAsc(String styleNumber);

    Optional<SkuImage> findFirstByStyleNumberAndShotTypeAndIsDeletedFalseOrderByPositionAsc(
            String styleNumber, ShotType shotType);
}