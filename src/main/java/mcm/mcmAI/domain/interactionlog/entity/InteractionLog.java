package mcm.mcmAI.domain.interactionlog.entity;

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
import mcm.mcmAI.domain.product.type.InterestType;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "interaction_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InteractionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interaction_id")
    private Long interactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku")
    private Sku sku;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", length = 30, nullable = false)
    private InterestType interestType;

    @Column(name = "sub_option", length = 50)
    private String subOption;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Builder
    public InteractionLog(
            Session session, Sku sku, InterestType interestType, String subOption, Integer durationSeconds
    ) {
        this.session = session;
        this.sku = sku;
        this.interestType = interestType;
        this.subOption = subOption;
        this.durationSeconds = durationSeconds;
    }
}