package mcm.mcmAI.domain.email.type;

public enum SentStatus {

    /** 수신자만 등록되고 아직 발송 전 */
    PENDING,

    /** 발송 성공 */
    SENT,

    /** 렌더링 또는 전송 실패 */
    FAILED
}
