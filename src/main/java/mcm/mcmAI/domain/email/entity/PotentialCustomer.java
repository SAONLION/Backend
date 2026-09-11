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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mcm.mcmAI.domain.email.type.SentStatus;
import mcm.mcmAI.domain.email.type.TriggerType;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "potential_customer")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PotentialCustomer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pc_id")
    private Long pcId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "language", length = 5)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", length = 20)
    private TriggerType triggerType;

    @Column(name = "consent_marketing")
    private Boolean consentMarketing;

    @Enumerated(EnumType.STRING)
    @Column(name = "sent_status", length = 20)
    private SentStatus sentStatus;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "store_name", length = 100)
    private String storeName;

    @Column(name = "fail_reason", length = 255)
    private String failReason;

    @Builder
    public PotentialCustomer(
            Session session, String email, String language, TriggerType triggerType,
            Boolean consentMarketing, String storeName
    ) {
        this.session = session;
        this.email = email;
        this.language = language;
        this.triggerType = triggerType;
        this.consentMarketing = consentMarketing;
        this.storeName = storeName;
        this.sentStatus = SentStatus.PENDING;
    }

    public void markSent(LocalDateTime sentAt) {
        this.sentStatus = SentStatus.SENT;
        this.sentAt = sentAt;
        this.failReason = null;
    }

    public void markFailed(String failReason) {
        this.sentStatus = SentStatus.FAILED;
        this.sentAt = null;
        // 컬럼 길이를 넘는 스택트레이스/예외 메시지가 그대로 들어와 저장이 실패하는 일이 없도록 자른다.
        this.failReason = failReason == null || failReason.length() <= 255
                ? failReason
                : failReason.substring(0, 255);
    }
}
