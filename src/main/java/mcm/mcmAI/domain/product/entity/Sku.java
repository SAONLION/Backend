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
@Table(name = "sku")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sku extends BaseEntity {

    @Id
    @Column(name = "sku")
    private Long sku;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "color", length = 30)
    private String color;

    @Column(name = "size", length = 255)
    private String size;

    @Column(name = "price")
    private Integer price;

    @Column(name = "stock_qty")
    private Integer stockQty;

    @Builder
    public Sku(Long sku, Long productId, String color, String size, Integer price, Integer stockQty) {
        this.sku = sku;
        this.productId = productId;
        this.color = color;
        this.size = size;
        this.price = price;
        this.stockQty = stockQty;
    }
}