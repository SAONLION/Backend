package mcm.mcmAI.domain.sku.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "sku")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sku extends BaseEntity {

    @Id
    @Column(name = "sku")
    private Long sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "color", length = 30)
    private String color;

    @Column(name = "size", length = 255)
    private String size;

    @Column(name = "price")
    private Integer price;

    @Column(name = "stock_qty")
    private Integer stockQty;

    @Column(name = "style_number", length = 20)
    private String styleNumber;

    @Builder
    public Sku(
            Long sku, Product product, String color, String size, Integer price, Integer stockQty,
            String styleNumber
    ) {
        this.sku = sku;
        this.product = product;
        this.color = color;
        this.size = size;
        this.price = price;
        this.stockQty = stockQty;
        this.styleNumber = styleNumber;
    }
}