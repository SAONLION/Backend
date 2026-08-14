package mcm.mcmAI.domain.product.dto;

import java.time.LocalDateTime;
import mcm.mcmAI.domain.product.entity.Product;

public record ProductResponseDTO(
        Long productId,
        String name,
        String category,
        String materialDesc,
        String heritageDesc,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProductResponseDTO from(Product product) {
        return new ProductResponseDTO(
                product.getProductId(),
                product.getName(),
                product.getCategory(),
                product.getMaterialDesc(),
                product.getHeritageDesc(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}