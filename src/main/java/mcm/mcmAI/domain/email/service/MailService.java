package mcm.mcmAI.domain.email.service;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcm.mcmAI.domain.email.entity.PotentialCustomer;
import mcm.mcmAI.domain.email.repository.PotentialCustomerRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실제 SMTP 발송 구현체.
 *
 * <p>spring.mail.* 값은 application-mail.yml(레포에 커밋되지 않는 로컬/서버 전용 파일 — .gitignore와
 * spring.config.import에 이미 등록돼 있다)에서 온다. Gmail 등 대부분의 SMTP는 인증 계정과 다른
 * From 주소로 보내면 거부되거나 From이 강제로 바뀌므로, app.email.from-address는 spring.mail.username과
 * 같은 주소(또는 해당 계정에 등록된 발신 별칭)로 맞춰야 한다.
 *
 * <p>app.email.sender=smtp일 때만 활성화된다. SMTP 호출이 초 단위로 걸릴 수 있어 요청 스레드를
 * 막지 않도록 {@link mcm.mcmAI.global.AsyncConfig}의 전용 스레드풀에서 비동기로 실행하며, 발송
 * 성공/실패는 이 메서드가 직접 potential_customer에 반영한다 — 정책상 발송 실패로 API 요청 자체를
 * 실패시키지 않으므로(예외를 던지지 않고 FAILED로 기록), 호출자는 이 메서드 호출 직후 성공 여부를
 * 알 수 없고 필요하면 sentStatus를 다시 조회해야 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.email.sender", havingValue = "smtp")
public class MailService implements EmailSender {

    private final JavaMailSender javaMailSender;
    private final PotentialCustomerRepository potentialCustomerRepository;
    private final EmailProperties emailProperties;

    @Override
    @Async("mailExecutor")
    @Transactional
    public void send(Long pcId, String toAddress, String subject, String htmlBody) {
        PotentialCustomer customer = potentialCustomerRepository.findById(pcId).orElse(null);
        if (customer == null) {
            // 발송 트리거와 이력 저장은 같은 트랜잭션에서 이미 커밋됐어야 하므로 정상적으로는
            // 일어나지 않는다. 방어적으로만 남긴다.
            log.warn("발송 대상 잠재고객을 찾을 수 없습니다 - pcId={}", pcId);
            return;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            // multipart=false: 템플릿의 이미지가 전부 절대 URL 참조라 첨부/인라인 리소스가 없다.
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(toAddress);
            helper.setFrom(emailProperties.getFromAddress());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            javaMailSender.send(message);
            customer.markSent(LocalDateTime.now());
            log.info("메일 발송 성공 - pcId={}, to={}", pcId, toAddress);
        } catch (Exception e) {
            // 발송 실패로 요청 자체를 실패시키지 않는다(정책). 이력을 FAILED로 남겨 재시도·모니터링
            // 대상이 되게 한다. @Async 메서드라 여기서 예외를 던져도 호출자에게 전달되지 않으므로
            // 반드시 이 안에서 상태를 기록해야 한다.
            log.error("메일 발송 실패 - pcId={}, to={}", pcId, toAddress, e);
            customer.markFailed(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }
}
