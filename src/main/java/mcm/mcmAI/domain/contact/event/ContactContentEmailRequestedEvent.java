package mcm.mcmAI.domain.contact.event;

/**
 * Contact 저장이 커밋된 뒤에 PotentialCustomerEmailService의 발송 파이프라인을 트리거하기 위한 이벤트.
 * ContactService의 트랜잭션 안에서 바로 호출하면, 그 파이프라인에서 나는 예외(추천/렌더링 오류 등)가
 * 같은 물리 트랜잭션을 rollback-only로 만들어 방금 저장한 Contact 행까지 함께 롤백된다 — 리드(이메일)
 * 확보라는 이 API의 핵심 목적이 발송 파이프라인의 실패에 발목 잡히면 안 되므로, 커밋 이후 별도
 * 트랜잭션에서만 처리한다.
 */
public record ContactContentEmailRequestedEvent(String sessionId, String email) {
}
