package mcm.mcmAI.domain.contact.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcm.mcmAI.domain.contact.dto.ContactRequest;
import mcm.mcmAI.domain.contact.dto.ContactResponse;
import mcm.mcmAI.domain.contact.entity.Contact;
import mcm.mcmAI.domain.contact.repository.ContactRepository;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactService {

    private final ContactRepository contactRepository;
    private final SessionRepository sessionRepository;

    @Transactional
    public ContactResponse createContact(String sessionId, ContactRequest request) {
        Session session = findSession(sessionId);
        validateEmail(request.email());

        LocalDateTime sentAt = LocalDateTime.now();

        Contact contact = Contact.builder()
                .session(session)
                .actionId(request.actionId())
                .productId(request.productId())
                .email(request.email())
                .contentTopic(request.contentTopic())
                .contentSent(true)
                .sentAt(sentAt)
                .build();

        Contact saved = contactRepository.save(contact);

        log.info(
                "콘텐츠 발송 - contactId={}, sessionId={}, email={}, contentTopic={}",
                saved.getContactId(), sessionId, saved.getEmail(), saved.getContentTopic()
        );

        return ContactResponse.from(saved);
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_CONTACT_INFO);
        }
    }

    private Session findSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }
}