package mcm.mcmAI.domain.qna.type;

import java.util.Arrays;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;

public enum QuestionType {

    AS_REPAIR,
    CARE,
    GIFT_WRAP,
    TAX_REFUND,
    SHIPPING_RETURN,
    FREE_TEXT;

    public static QuestionType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_QUESTION_TYPE));
    }
}
