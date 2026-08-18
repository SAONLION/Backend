package mcm.mcmAI.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 제품을 찾을 수 없습니다."),
    OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 옵션을 찾을 수 없습니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."),
    ACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 액션을 찾을 수 없습니다."),
    INVALID_RESPONSE_KEY(HttpStatus.BAD_REQUEST, "허용되지 않은 응답 값입니다."),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "이메일 형식을 확인해주세요."),
    MISSING_CONTACT_INFO(HttpStatus.BAD_REQUEST, "이메일을 작성해주세요."),
    MISSING_PRODUCT_ID(HttpStatus.BAD_REQUEST, "제품 ID를 입력해주세요."),
    CALL_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 호출을 찾을 수 없습니다."),
    INVALID_CALL_STATUS(HttpStatus.BAD_REQUEST, "허용되지 않은 상태 값입니다."),
    SKU_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 SKU를 찾을 수 없습니다."),
    INVALID_PICKUP_METHOD(HttpStatus.BAD_REQUEST, "허용되지 않은 수령 방법입니다."),
    INVALID_QUESTION_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않은 문의 유형입니다."),
    MISSING_QNA_QUESTION(HttpStatus.BAD_REQUEST, "질문 내용을 입력해주세요.");

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