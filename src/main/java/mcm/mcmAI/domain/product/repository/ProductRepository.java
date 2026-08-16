package mcm.mcmAI.domain.product.repository;

import java.util.List;
import mcm.mcmAI.domain.product.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

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
}