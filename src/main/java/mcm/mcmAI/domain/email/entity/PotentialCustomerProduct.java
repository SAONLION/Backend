package mcm.mcmAI.domain.email.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mcm.mcmAI.domain.email.type.SlotType;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.global.entity.BaseEntity;

/**
 * 메일 한 통에 실제로 실린 상품 슬롯 스냅샷.
 * 상품명/이미지 URL을 함께 복사해 두는 것은, 이후 카탈로그가 바뀌거나 SKU가 삭제돼도
 * 고객이 받은 메일 본문을 그대로 재현할 수 있어야 하기 때문이다.
 */
@Getter
@Entity
@Table(name = "potential_customer_product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PotentialCustomerProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pc_id")
    private PotentialCustomer potentialCustomer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku")
    private Sku sku;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_type", length = 20, nullable = false)
    private SlotType slotType;

    @Column(name = "slot_order", nullable = false)
    private Integer slotOrder;

    @Column(name = "product_name", length = 100)
    private String productName;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder
    public PotentialCustomerProduct(
            PotentialCustomer potentialCustomer, Sku sku, SlotType slotType,
            Integer slotOrder, String productName, String imageUrl
    ) {
        this.potentialCustomer = potentialCustomer;
        this.sku = sku;
        this.slotType = slotType;
        this.slotOrder = slotOrder;
        this.productName = productName;
        this.imageUrl = imageUrl;
    }
}
