package mcm.mcmAI.domain.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mcm.mcmAI.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "category", length = 50, nullable = false)
    private String category;

    @Column(name = "material_desc", columnDefinition = "TEXT")
    private String materialDesc;

    @Column(name = "heritage_desc", columnDefinition = "TEXT")
    private String heritageDesc;

    @Builder
    public Product(String name, String category, String materialDesc, String heritageDesc) {
        this.name = name;
        this.category = category;
        this.materialDesc = materialDesc;
        this.heritageDesc = heritageDesc;
    }

    public void update(String name, String category, String materialDesc, String heritageDesc) {
        this.name = name;
        this.category = category;
        this.materialDesc = materialDesc;
        this.heritageDesc = heritageDesc;
    }
}