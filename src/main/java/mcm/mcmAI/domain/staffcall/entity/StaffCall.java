package mcm.mcmAI.domain.staffcall.entity;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.staffcall.type.StaffCallStatus;
import mcm.mcmAI.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "staff_call")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffCall extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "call_id")
    private Long callId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "reason", length = 200, nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private StaffCallStatus status;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Builder
    public StaffCall(Session session, Product product, String reason) {
        this.session = session;
        this.product = product;
        this.reason = reason;
        this.status = StaffCallStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
    }

    public void changeStatus(StaffCallStatus status) {
        this.status = status;
    }
}