package mcm.mcmAI.domain.email.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcm.mcmAI.domain.email.dto.EmailContentResponse;
import mcm.mcmAI.domain.email.dto.EmailSendRequest;
import mcm.mcmAI.domain.email.dto.EmailSendResponse;
import mcm.mcmAI.domain.email.dto.EmailSlotItem;
import mcm.mcmAI.domain.email.entity.PotentialCustomer;
import mcm.mcmAI.domain.email.entity.PotentialCustomerProduct;
import mcm.mcmAI.domain.email.event.MailDispatchRequestedEvent;
import mcm.mcmAI.domain.email.repository.PotentialCustomerProductRepository;
import mcm.mcmAI.domain.email.repository.PotentialCustomerRepository;
import mcm.mcmAI.domain.email.type.SentStatus;
import mcm.mcmAI.domain.email.type.SlotType;
import mcm.mcmAI.domain.email.type.TriggerType;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 수신자 등록 → 본문 구성 → 렌더링 → 발송 트리거 → 이력 기록까지를 묶는다.
 *
 * <p>실제 발송(EmailSender.send)은 이 클래스의 트랜잭션이 커밋된 "다음"에만 실행된다
 * ({@link #onMailDispatchRequested} 참고). 트랜잭션이 열려 있는 동안 곧바로 발송을 트리거하면,
 * 비동기 구현체(MailService)가 다른 스레드에서 pcId로 PotentialCustomer를 조회할 때 그 행이
 * 아직 커밋되지 않아 못 찾는 경쟁 상태가 생길 수 있기 때문이다. 그 결과 {@link #send}가
 * 반환하는 sentStatus는 항상 PENDING이며(발송은 커밋 이후에 비동기로 진행), 최종 성공/실패는
 * {@link #getStatus}로 다시 조회해야 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PotentialCustomerEmailService {

    private static final String DEFAULT_LANGUAGE = "ko";
    /** 세션당 허용하는 최대 발송 횟수. FAILED는 실제로 나간 게 아니므로 포함하지 않는다. */
    private static final int MAX_DISPATCH_PER_SESSION = 5;

    private final SessionRepository sessionRepository;
    private final SkuRepository skuRepository;
    private final PotentialCustomerRepository potentialCustomerRepository;
    private final PotentialCustomerProductRepository potentialCustomerProductRepository;
    private final EmailContentService emailContentService;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final EmailSender emailSender;
    private final EmailProperties emailProperties;
    private final ApplicationEventPublisher eventPublisher;

    public EmailContentResponse getContent(String sessionId) {
        return emailContentService.buildContent(sessionId);
    }

    public String getRenderedHtml(String sessionId) {
        return emailTemplateRenderer.render(emailContentService.buildContent(sessionId));
    }

    @Transactional
    public EmailSendResponse send(String sessionId, EmailSendRequest request) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        validate(request);

        long dispatchedCount =
                potentialCustomerRepository.countBySession_SessionIdAndSentStatusNot(sessionId, SentStatus.FAILED);
        if (dispatchedCount >= MAX_DISPATCH_PER_SESSION) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_LIMIT_EXCEEDED);
        }

        EmailContentResponse content = emailContentService.buildContent(sessionId);

        PotentialCustomer customer = potentialCustomerRepository.save(PotentialCustomer.builder()
                .session(session)
                .email(request.email())
                .language(resolveLanguage(request, session))
                .triggerType(request.triggerType() != null ? request.triggerType() : TriggerType.SESSION_END)
                .consentMarketing(request.consentMarketing())
                .storeName(content.storeName())
                .build());

        saveSlots(customer, content);

        String html = emailTemplateRenderer.render(content);
        String subject = emailProperties.buildSubject(content.nickname());
        // 이 시점엔 아직 커밋 전이라 바로 보내지 않고, 커밋 이후에 처리되도록 이벤트만 발행한다.
        eventPublisher.publishEvent(new MailDispatchRequestedEvent(customer.getPcId(), request.email(), subject, html));

        return EmailSendResponse.of(
                customer,
                content.filledPickCount(),
                (int) content.recommendations().stream().filter(EmailSlotItem::isFilled).count()
        );
    }

    /**
     * 커밋 이후에만 실행된다. EmailSender 구현체가 동기(로그 전용)든 비동기(MailService)든,
     * 이 시점부터는 potential_customer 행이 항상 조회 가능한 상태임이 보장된다.
     * fallbackExecution=true는 트랜잭션 없이 send()가 호출되는 예외적인 상황(예: 테스트)에서
     * 이벤트가 조용히 버려지지 않고 즉시 실행되도록 하는 안전장치다.
     *
     * <p>AFTER_COMMIT 시점엔 원래 트랜잭션이 이미 끝나 있어 합류할 트랜잭션이 없으므로,
     * Spring이 @TransactionalEventListener 메서드에는 REQUIRES_NEW/NOT_SUPPORTED 외의
     * propagation(클래스 레벨 기본값인 REQUIRED 포함)을 금지한다. 새 트랜잭션을 명시한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMailDispatchRequested(MailDispatchRequestedEvent event) {
        emailSender.send(event.pcId(), event.toAddress(), event.subject(), event.htmlBody());
    }

    /** 발송 트리거(POST /send) 이후 최종 발송 상태를 확인한다. */
    public EmailSendResponse getStatus(Long pcId) {
        PotentialCustomer customer = potentialCustomerRepository.findById(pcId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POTENTIAL_CUSTOMER_NOT_FOUND));
        return buildResponse(customer);
    }

    private EmailSendResponse buildResponse(PotentialCustomer customer) {
        List<PotentialCustomerProduct> slots = potentialCustomerProductRepository
                .findByPotentialCustomer_PcIdOrderBySlotTypeAscSlotOrderAsc(customer.getPcId());
        int filledPickCount = (int) slots.stream().filter(slot -> slot.getSlotType() == SlotType.PICK).count();
        int filledRecommendCount =
                (int) slots.stream().filter(slot -> slot.getSlotType() == SlotType.RECOMMEND).count();

        return EmailSendResponse.of(customer, filledPickCount, filledRecommendCount);
    }

    /** 메일에 실제로 실린 슬롯만 스냅샷으로 남긴다(빈 자리는 기록할 것이 없다). */
    private void saveSlots(PotentialCustomer customer, EmailContentResponse content) {
        List<EmailSlotItem> slots = new ArrayList<>(content.picks());
        slots.addAll(content.recommendations());

        List<PotentialCustomerProduct> rows = slots.stream()
                .filter(EmailSlotItem::isFilled)
                .map(slot -> PotentialCustomerProduct.builder()
                        .potentialCustomer(customer)
                        .sku(skuRepository.getReferenceById(slot.skuId()))
                        .slotType(slot.slotType())
                        .slotOrder(slot.slotOrder())
                        .productName(slot.productName())
                        .imageUrl(slot.imageUrl())
                        .build())
                .toList();

        potentialCustomerProductRepository.saveAll(rows);
    }

    private void validate(EmailSendRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_CONTACT_INFO);
        }
        if (!request.consentMarketing()) {
            throw new BusinessException(ErrorCode.MARKETING_CONSENT_REQUIRED);
        }
    }

    private String resolveLanguage(EmailSendRequest request, Session session) {
        if (request.language() != null && !request.language().isBlank()) {
            return request.language();
        }
        return session.getLanguage() != null ? session.getLanguage() : DEFAULT_LANGUAGE;
    }
}
