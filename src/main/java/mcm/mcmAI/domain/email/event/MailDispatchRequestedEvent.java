package mcm.mcmAI.domain.email.event;

/**
 * potential_customer/potential_customer_product 저장이 커밋된 뒤에 실제 메일 발송을 트리거하기
 * 위한 이벤트. {@link mcm.mcmAI.domain.email.service.PotentialCustomerEmailService}의 트랜잭션이
 * 아직 열려 있는 동안 EmailSender를 직접 호출하면, 비동기 구현체(MailService)가 다른 스레드/트랜잭션에서
 * pcId를 조회할 때 그 행이 아직 커밋되지 않아 못 찾을 수 있다. 이 이벤트를 AFTER_COMMIT 시점에만
 * 처리하게 해 그 경쟁 상태를 막는다.
 */
public record MailDispatchRequestedEvent(Long pcId, String toAddress, String subject, String htmlBody) {
}
