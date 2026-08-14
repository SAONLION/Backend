package mcm.mcmAI.domain.session.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mcm.mcmAI.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "session")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Session extends BaseEntity {

    @Id
    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "language", length = 5)
    private String language;

    @Column(name = "intent_score")
    private Integer intentScore;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "end_reason", length = 20)
    private String endReason;

    @Builder
    public Session(String sessionId, String language) {
        this.sessionId = sessionId;
        this.language = language;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void end() {
        if (this.endedAt == null) {
            this.endedAt = LocalDateTime.now();
        }
    }
}
