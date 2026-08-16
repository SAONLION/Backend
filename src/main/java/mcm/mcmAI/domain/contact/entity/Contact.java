package mcm.mcmAI.domain.contact.entity;

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
import mcm.mcmAI.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "contact")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contact extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contact_id")
    private Long contactId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @Column(name = "action_id")
    private Long actionId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "content_topic", length = 200)
    private String contentTopic;

    @Column(name = "content_sent", nullable = false)
    private boolean contentSent;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Builder
    public Contact(
            Session session, Long actionId, Long productId,
            String email, String contentTopic, boolean contentSent, LocalDateTime sentAt
    ) {
        this.session = session;
        this.actionId = actionId;
        this.productId = productId;
        this.email = email;
        this.contentTopic = contentTopic;
        this.contentSent = contentSent;
        this.sentAt = sentAt;
    }
}