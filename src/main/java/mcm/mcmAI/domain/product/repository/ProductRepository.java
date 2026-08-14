package mcm.mcmAI.domain.product.repository;

import mcm.mcmAI.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}