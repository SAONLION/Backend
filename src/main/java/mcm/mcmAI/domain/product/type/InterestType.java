package mcm.mcmAI.domain.product.type;

import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "유효하지 않은 interestType 입니다: " + value));
    }
}