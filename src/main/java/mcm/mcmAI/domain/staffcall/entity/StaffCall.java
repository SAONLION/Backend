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
import mcm.mcmAI.domain.pendingaction.entity.PendingAction;
import mcm.mcmAI.domain.purchaseinquiry.entity.PurchaseInquiry;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.staffcall.type.StaffCallStatus;
import mcm.mcmAI.domain.tryonrequest.entity.TryonRequest;
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
    @JoinColumn(name = "sku")
    private Sku sku;

    @Column(name = "reason", length = 200, nullable = false)
    private String reason;

    @Column(name = "size", length = 255)
    private String size;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tryon_request_id")
    private TryonRequest tryonRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_inquiry_id")
    private PurchaseInquiry purchaseInquiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pending_action_id")
    private PendingAction pendingAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private StaffCallStatus status;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Builder
    public StaffCall(
            Session session, Sku sku, String reason, String size,
            TryonRequest tryonRequest, PurchaseInquiry purchaseInquiry, PendingAction pendingAction
    ) {
        this.session = session;
        this.sku = sku;
        this.reason = reason;
        this.size = size;
        this.tryonRequest = tryonRequest;
        this.purchaseInquiry = purchaseInquiry;
        this.pendingAction = pendingAction;
        this.status = StaffCallStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
    }

    public void changeStatus(StaffCallStatus status) {
        this.status = status;
    }

    public void changeRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
}