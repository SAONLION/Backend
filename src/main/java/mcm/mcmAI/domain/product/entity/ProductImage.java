package mcm.mcmAI.domain.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mcm.mcmAI.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "product_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends BaseEntity {

    @Id
    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "sku")
    private Long sku;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Builder
    public ProductImage(Long imageId, Long sku, String imageUrl, Integer sortOrder) {
        this.imageId = imageId;
        this.sku = sku;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }
}