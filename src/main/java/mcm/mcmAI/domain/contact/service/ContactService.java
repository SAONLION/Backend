package mcm.mcmAI.domain.contact.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcm.mcmAI.domain.contact.dto.ContactRequest;
import mcm.mcmAI.domain.contact.dto.ContactResponse;
import mcm.mcmAI.domain.contact.entity.Contact;
import mcm.mcmAI.domain.contact.event.ContactContentEmailRequestedEvent;
import mcm.mcmAI.domain.contact.repository.ContactRepository;
import mcm.mcmAI.domain.email.dto.EmailSendRequest;
import mcm.mcmAI.domain.email.service.PotentialCustomerEmailService;
import mcm.mcmAI.domain.email.type.TriggerType;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactService {

    private final ContactRepository contactRepository;
    private final SessionRepository sessionRepository;
    private final PotentialCustomerEmailService potentialCustomerEmailService;
    private final ApplicationEventPublisher eventPublisher;

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

        // 이 시점엔 아직 커밋 전이라 바로 발송을 트리거하지 않고, 커밋 이후에 처리되도록 이벤트만 발행한다.
        eventPublisher.publishEvent(new ContactContentEmailRequestedEvent(sessionId, request.email()));

        log.info(
                "콘텐츠 발송 - contactId={}, sessionId={}, email={}, contentTopic={}",
                saved.getContactId(), sessionId, saved.getEmail(), saved.getContentTopic()
        );

        return ContactResponse.from(saved);
    }

    /**
     * 커밋 이후에만 실행된다. 실제 발송은 PotentialCustomerEmailService의 기존 파이프라인
     * (추천 콘텐츠 구성 → 템플릿 렌더링 → EmailSender 비동기 발송, 세션당 최대 5회 한도 포함)을 그대로
     * 재사용한다. CB5/CB6 팝업에서 "콘텐츠 받을래요"를 선택한 행위 자체가 콘텐츠 수신 동의이므로
     * consentMarketing은 항상 true로 넘긴다. 이 파이프라인에서 예외가 나도(세션당 발송 한도 초과 포함)
     * Contact 저장(리드 확보)은 이미 커밋되어 있으므로 API 응답에는 영향이 없고, 여기서는 로그만 남긴다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onContentEmailRequested(ContactContentEmailRequestedEvent event) {
        try {
            potentialCustomerEmailService.send(
                    event.sessionId(),
                    new EmailSendRequest(event.email(), true, TriggerType.CONTENT_REQUEST, null)
            );
        } catch (Exception e) {
            log.error(
                    "콘텐츠 요청 메일 발송 트리거 실패 - sessionId={}, email={}",
                    event.sessionId(), event.email(), e
            );
        }
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
