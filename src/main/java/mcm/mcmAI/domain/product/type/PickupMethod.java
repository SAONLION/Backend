package mcm.mcmAI.domain.product.type;

import java.util.Arrays;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;

public enum PickupMethod {

    HOTEL_DELIVERY("호텔로 배송"),
    AIRPORT_PICKUP("공항에서 수령"),
    HOME_COUNTRY_DELIVERY("귀국지로 배송"),
    IN_STORE_PICKUP("매장에서 바로 수령");

    private final String label;

    PickupMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static PickupMethod from(String value) {
        return Arrays.stream(values())
                .filter(method -> method.label.equals(value))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PICKUP_METHOD));
    }
}