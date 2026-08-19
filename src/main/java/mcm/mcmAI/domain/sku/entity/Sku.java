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

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    @Column(name = "body_material", length = 255)
    private String bodyMaterial;

    @Column(name = "trim_material", length = 255)
    private String trimMaterial;

    @Column(name = "country_of_origin", length = 100)
    private String countryOfOrigin;

    @Column(name = "dimensions_text", length = 255)
    private String dimensionsText;

    @Column(name = "storage_text", columnDefinition = "TEXT")
    private String storageText;

    @Column(name = "lining_care_text", columnDefinition = "TEXT")
    private String liningCareText;

    @Column(name = "strap_length", length = 64)
    private String strapLength;

    @Column(name = "handle_drop", length = 64)
    private String handleDrop;

    @Column(name = "hardware_text", columnDefinition = "TEXT")
    private String hardwareText;

    @Column(name = "sustainability_certification", length = 255)
    private String sustainabilityCertification;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Builder
    public Sku(
            Long sku, Product product, String color, String size, Integer price, Integer stockQty,
            String styleNumber, String description, String shortDescription, String bodyMaterial,
            String trimMaterial, String countryOfOrigin, String dimensionsText, String storageText,
            String liningCareText, String strapLength, String handleDrop, String hardwareText,
            String sustainabilityCertification
    ) {
        this.sku = sku;
        this.product = product;
        this.color = color;
        this.size = size;
        this.price = price;
        this.stockQty = stockQty;
        this.styleNumber = styleNumber;
        this.description = description;
        this.shortDescription = shortDescription;
        this.bodyMaterial = bodyMaterial;
        this.trimMaterial = trimMaterial;
        this.countryOfOrigin = countryOfOrigin;
        this.dimensionsText = dimensionsText;
        this.storageText = storageText;
        this.liningCareText = liningCareText;
        this.strapLength = strapLength;
        this.handleDrop = handleDrop;
        this.hardwareText = hardwareText;
        this.sustainabilityCertification = sustainabilityCertification;
    }
}