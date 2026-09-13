package mcm.mcmAI.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 제품을 찾을 수 없습니다."),
    OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 옵션을 찾을 수 없습니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."),
    SESSION_ALREADY_ENDED(HttpStatus.CONFLICT, "이미 종료된 세션입니다."),
    ACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 액션을 찾을 수 없습니다."),
    INVALID_RESPONSE_KEY(HttpStatus.BAD_REQUEST, "허용되지 않은 응답 값입니다."),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "이메일 형식을 확인해주세요."),
    MISSING_CONTACT_INFO(HttpStatus.BAD_REQUEST, "이메일을 작성해주세요."),
    CALL_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 호출을 찾을 수 없습니다."),
    INVALID_CALL_STATUS(HttpStatus.BAD_REQUEST, "허용되지 않은 상태 값입니다."),
    SKU_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 SKU를 찾을 수 없습니다."),
    TRYON_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 착장 요청을 찾을 수 없습니다."),
    INVALID_PICKUP_METHOD(HttpStatus.BAD_REQUEST, "허용되지 않은 수령 방법입니다."),
    INVALID_QUESTION_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않은 문의 유형입니다."),
    MISSING_QNA_QUESTION(HttpStatus.BAD_REQUEST, "질문 내용을 입력해주세요."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "닉네임은 1자 이상 21자 이하로 입력해주세요."),
    INVALID_PURPOSE(HttpStatus.BAD_REQUEST, "허용되지 않은 방문 목적입니다."),
    INVALID_INTEREST_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않은 관심 유형입니다."),
    MISSING_SESSION_ID(HttpStatus.BAD_REQUEST, "세션 ID를 입력해주세요."),
    STAFF_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "직원 인증 토큰이 유효하지 않습니다."),
    MARKETING_CONSENT_REQUIRED(HttpStatus.BAD_REQUEST, "마케팅 정보 수신에 동의해주세요."),
    EMAIL_TEMPLATE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "메일 템플릿을 읽을 수 없습니다."),
    POTENTIAL_CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 발송 이력을 찾을 수 없습니다."),
    EMAIL_SEND_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "세션당 발송 가능한 메일 횟수를 초과했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}