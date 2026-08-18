package mcm.mcmAI.domain.visitpurpose.type;

import java.util.Arrays;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;

public enum PurposeType {

    TRAVEL("여행"),
    GIFT("선물"),
    BROWSING("구경"),
    OTHER("기타");

    private final String label;

    PurposeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static PurposeType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PURPOSE));
    }
}
