package mcm.mcmAI.domain.tagscanlog.entity;

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
@Table(name = "tag_scan_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TagScanLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scan_id")
    private Long scanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku")
    private Sku sku;

    @Column(name = "scan_order")
    private Integer scanOrder;

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    @Builder
    public TagScanLog(Session session, Sku sku, Integer scanOrder, LocalDateTime scannedAt) {
        this.session = session;
        this.sku = sku;
        this.scanOrder = scanOrder;
        this.scannedAt = scannedAt;
    }
}