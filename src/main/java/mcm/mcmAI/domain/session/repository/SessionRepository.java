package mcm.mcmAI.domain.session.repository;

import mcm.mcmAI.domain.session.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, String> {
}