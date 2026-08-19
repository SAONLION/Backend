package mcm.mcmAI.domain.skuimage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mcm.mcmAI.domain.skuimage.type.ShotType;
import mcm.mcmAI.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "sku_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkuImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "style_number", length = 20, nullable = false)
    private String styleNumber;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "shot_type", length = 20, nullable = false)
    private ShotType shotType;

    @Column(name = "has_person", nullable = false)
    private boolean hasPerson;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Builder
    public SkuImage(String styleNumber, Integer position, String imageUrl, ShotType shotType, boolean hasPerson) {
        this.styleNumber = styleNumber;
        this.position = position;
        this.imageUrl = imageUrl;
        this.shotType = shotType;
        this.hasPerson = hasPerson;
    }
}