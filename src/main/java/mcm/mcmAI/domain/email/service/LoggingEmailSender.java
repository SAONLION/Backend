package mcm.mcmAI.domain.email.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcm.mcmAI.domain.email.repository.PotentialCustomerRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기본 구현. 실제 SMTP 발송 없이 로그만 남기고 즉시 SENT로 기록한다.
 * app.email.sender가 smtp가 아닐 때(로컬 개발, application-mail.yml이 없는 환경 등) 쓰인다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.email.sender", havingValue = "log", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    private final PotentialCustomerRepository potentialCustomerRepository;

    @Override
    @Transactional
    public void send(Long pcId, String toAddress, String subject, String htmlBody) {
        log.info(
                "개인화 추천 메일 발송(로그 전용) - pcId={}, to={}, subject={}, bodyLength={}",
                pcId, toAddress, subject, htmlBody.length()
        );
        potentialCustomerRepository.findById(pcId)
                .ifPresent(customer -> customer.markSent(LocalDateTime.now()));
    }
}
