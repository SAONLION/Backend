package mcm.mcmAI.domain.contact.repository;

import java.util.List;
import mcm.mcmAI.domain.contact.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findBySession_SessionId(String sessionId);
}