package mcm.mcmAI.domain.sku.repository;

import java.util.List;
import java.util.Optional;
import mcm.mcmAI.domain.sku.entity.Sku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkuRepository extends JpaRepository<Sku, Long> {

    List<Sku> findByProduct_ProductIdAndIsDeletedFalseOrderBySkuAsc(Long productId);

    Optional<Sku> findBySkuAndIsDeletedFalse(Long sku);

    // category가 null이면 전체 카탈로그에서, 아니면 해당 카테고리(product.category 완전 일치) 안에서 무작위 SKU 하나를 고른다.
    @Query(value = """
            SELECT s.*
            FROM sku s
            JOIN product p ON p.product_id = s.product_id
            WHERE s.is_deleted = FALSE
              AND (:category IS NULL OR p.category = :category)
            ORDER BY RAND()
            LIMIT 1
            """, nativeQuery = true)
    Optional<Sku> findRandomActiveByCategory(@Param("category") String category);

    // product.category로 분류하기 위해 product를 함께 조회한다(N+1 방지).
    @Query("SELECT s FROM Sku s JOIN FETCH s.product WHERE s.isDeleted = FALSE")
    List<Sku> findAllActiveWithProduct();
}