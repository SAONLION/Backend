package mcm.mcmAI.domain.email.service;

/**
 * 렌더링이 끝난 HTML 본문을 실제로 전송하는 지점.
 * 발송 채널(로그 전용, SMTP, 이후 SES 등)을 갈아 끼워도 수집·렌더링 로직이 영향을 받지 않도록
 * 분리했다. 구현체는 발송 성공/실패 여부를 pcId로 조회한 PotentialCustomer에 직접 반영해야
 * 한다(성공 시 markSent, 실패 시 markFailed) — 비동기 구현체(MailService)는 이 메서드가
 * 반환된 시점에 아직 발송이 끝나지 않았을 수 있어, 호출자가 반환 직후 상태를 판단할 수 없기
 * 때문이다.
 */
public interface EmailSender {

    void send(Long pcId, String toAddress, String subject, String htmlBody);
}
