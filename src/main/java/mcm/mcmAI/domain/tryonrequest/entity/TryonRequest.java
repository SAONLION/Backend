package mcm.mcmAI.domain.tryonrequest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "tryon_request")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TryonRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tryon_request_id")
    private Long tryonRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku")
    private Sku sku;

    @Column(name = "size", length = 255, nullable = false)
    private String size;

    @Column(name = "color", length = 30, nullable = false)
    private String color;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Builder
    public TryonRequest(Session session, Sku sku, String size, String color) {
        this.session = session;
        this.sku = sku;
        this.size = size;
        this.color = color;
        this.requestedAt = LocalDateTime.now();
    }
}
