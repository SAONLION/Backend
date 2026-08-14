package mcm.mcmAI.domain.visitpurpose.entity;

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
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.visitpurpose.type.PurposeType;
import mcm.mcmAI.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "visit_purpose")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitPurpose extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purpose_id")
    private Long purposeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose_type", length = 30, nullable = false)
    private PurposeType purposeType;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Builder
    public VisitPurpose(Session session, PurposeType purposeType, LocalDateTime confirmedAt) {
        this.session = session;
        this.purposeType = purposeType;
        this.confirmedAt = confirmedAt;
    }
}