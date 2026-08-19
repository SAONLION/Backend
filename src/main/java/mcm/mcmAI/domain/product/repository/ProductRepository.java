package mcm.mcmAI.domain.product.repository;

import java.util.Collection;
import java.util.List;
import mcm.mcmAI.domain.product.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByEmbeddingIsNull();

    List<Product> findByEmbeddingIsNotNullAndProductIdNotIn(Collection<Long> excludeProductIds);

    @Query(value = """
            SELECT p.*
            FROM product p
            JOIN (
                SELECT product_id, MIN(price) AS min_price
                FROM sku
                WHERE price IS NOT NULL
                GROUP BY product_id
            ) sp ON sp.product_id = p.product_id
            WHERE p.category = :category
              AND p.product_id NOT IN (:excludeProductIds)
              AND sp.min_price BETWEEN :minPrice AND :maxPrice
            ORDER BY ABS(sp.min_price - :referencePrice) ASC
            """, nativeQuery = true)
    List<Product> findRecommendationCandidates(
            @Param("category") String category,
            @Param("excludeProductIds") List<Long> excludeProductIds,
            @Param("minPrice") int minPrice,
            @Param("maxPrice") int maxPrice,
            @Param("referencePrice") int referencePrice,
            Pageable pageable
    );

    // 카테고리/가격대 규칙으로도 후보를 채우지 못했을 때 쓰는 최후 폴백: 전체 세션에서 가장 많이 태그 스캔된(인기) 순으로 채운다.
    @Query(value = """
            SELECT p.*
            FROM product p
            LEFT JOIN (
                SELECT s.product_id AS product_id, COUNT(*) AS scan_count
                FROM tag_scan_log t
                JOIN sku s ON s.sku = t.sku
                GROUP BY s.product_id
            ) sc ON sc.product_id = p.product_id
            WHERE p.product_id NOT IN (:excludeProductIds)
            ORDER BY COALESCE(sc.scan_count, 0) DESC, p.product_id ASC
            """, nativeQuery = true)
    List<Product> findPopularProductCandidates(
            @Param("excludeProductIds") List<Long> excludeProductIds,
            Pageable pageable
    );
}
