package mcm.mcmAI.domain.staffcall.type;

import java.util.Arrays;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;

public enum StaffCallStatus {

    REQUESTED("직원 호출을 요청했어요"),
    ACKNOWLEDGED("직원이 확인했어요"),
    IN_PROGRESS("직원이 제품을 준비해서 가는 중이에요"),
    COMPLETED("직원이 도착했어요");

    private final String displayMessage;

    StaffCallStatus(String displayMessage) {
        this.displayMessage = displayMessage;
    }

    public String getDisplayMessage() {
        return displayMessage;
    }

    public static StaffCallStatus from(String value) {
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CALL_STATUS));
    }
}