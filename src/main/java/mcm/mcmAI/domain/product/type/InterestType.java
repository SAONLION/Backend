package mcm.mcmAI.domain.product.type;

import java.util.Arrays;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;

public enum InterestType {

    PRODUCT_UNDERSTANDING("상품 이해"),
    FIT_PREFERENCE("핏/취향 선호"),
    PURCHASE_CONDITION("구매 조건"),
    OTHER("기타");

    private final String label;

    InterestType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static InterestType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INTEREST_TYPE));
    }
}