package mcm.mcmAI.domain.email.type;

public enum TriggerType {

    /** 세션 종료 시점에 고객이 직접 메일 수신에 동의한 경우 */
    SESSION_END,

    /** CB5/CB6 팝업에서 "콘텐츠 받을래요"를 선택해 콘텐츠 수신에 동의한 경우 */
    CONTENT_REQUEST,

    /** SA/직원이 대시보드에서 수동으로 발송한 경우 */
    STAFF,

    /** 내부 테스트·미리보기 발송 */
    TEST
}
