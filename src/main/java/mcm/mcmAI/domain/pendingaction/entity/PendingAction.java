package mcm.mcmAI.domain.pendingaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mcm.mcmAI.domain.interactionlog.entity.InteractionLog;
import mcm.mcmAI.domain.pendingaction.type.BlockerType;
import mcm.mcmAI.domain.pendingaction.type.PendingActionStatus;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.staffcall.entity.StaffCall;
import mcm.mcmAI.domain.tagscanlog.entity.TagScanLog;
import mcm.mcmAI.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "pending_action")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PendingAction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id")
    private Long actionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @Enumerated(EnumType.STRING)
    @Column(name = "blocker_type", length = 20, nullable = false)
    private BlockerType blockerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_call_id")
    private StaffCall staffCall;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trigger_tag_scan_log_id")
    private TagScanLog triggerTagScanLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trigger_interaction_log_id")
    private InteractionLog triggerInteractionLog;

    @Column(name = "trigger_id", length = 20)
    private String triggerId;

    @Column(name = "popup_title", length = 200, nullable = false)
    private String popupTitle;

    @Column(name = "popup_body", columnDefinition = "TEXT")
    private String popupBody;

    @Convert(converter = PendingActionOptionListConverter.class)
    @Column(name = "options", columnDefinition = "TEXT", nullable = false)
    private List<PendingActionOption> options;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private PendingActionStatus status;

    @Column(name = "response_key", length = 50)
    private String responseKey;

    @Builder
    public PendingAction(
            Session session, BlockerType blockerType, Product product, StaffCall staffCall,
            TagScanLog triggerTagScanLog, InteractionLog triggerInteractionLog, String triggerId,
            String popupTitle, String popupBody, List<PendingActionOption> options
    ) {
        this.session = session;
        this.blockerType = blockerType;
        this.product = product;
        this.staffCall = staffCall;
        this.triggerTagScanLog = triggerTagScanLog;
        this.triggerInteractionLog = triggerInteractionLog;
        this.triggerId = triggerId;
        this.popupTitle = popupTitle;
        this.popupBody = popupBody;
        this.options = options;
        this.status = PendingActionStatus.PENDING;
    }

    public Optional<PendingActionOption> findOption(String key) {
        return options.stream()
                .filter(option -> option.key().equals(key))
                .findFirst();
    }

    public void respond(String responseKey) {
        this.responseKey = responseKey;
        this.status = PendingActionStatus.RESPONDED;
    }
}