package mcm.mcmAI.domain.product.type;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import mcm.mcmAI.domain.product.dto.HubOptionDTO;
import mcm.mcmAI.domain.product.dto.SubOptionDTO;

public final class HubOptionProvider {

    private static final Map<InterestType, List<SubOptionDTO>> SUB_OPTIONS = Map.of(
            InterestType.PRODUCT_UNDERSTANDING, List.of(
                    new SubOptionDTO(1L, "소재 안내", "MATERIAL"),
                    new SubOptionDTO(2L, "헤리티지/브랜드 스토리", "HERITAGE"),
                    new SubOptionDTO(3L, "관리 방법", "CARE"),
                    new SubOptionDTO(4L, "원산지", "ORIGIN")
            ),
            InterestType.FIT_PREFERENCE, List.of(
                    new SubOptionDTO(5L, "사이즈 가이드", "SIZE_GUIDE"),
                    new SubOptionDTO(6L, "컬러 옵션", "COLOR_OPTION"),
                    new SubOptionDTO(7L, "다른 제품과 비교", "COMPARE"),
                    new SubOptionDTO(8L, "스타일링 추천", "STYLING")
            ),
            InterestType.PURCHASE_CONDITION, List.of(
                    new SubOptionDTO(9L, "가격 안내", "PRICE"),
                    new SubOptionDTO(10L, "재고 확인", "STOCK"),
                    new SubOptionDTO(11L, "할인/프로모션", "PROMOTION"),
                    new SubOptionDTO(12L, "매장 픽업", "PICKUP")
            ),
            InterestType.OTHER, List.of(
                    new SubOptionDTO(13L, "직원 호출", "CALL_STAFF"),
                    new SubOptionDTO(14L, "교환/환불 문의", "EXCHANGE_REFUND"),
                    new SubOptionDTO(15L, "기타 문의", "ETC")
            )
    );

    private HubOptionProvider() {
    }

    public static List<HubOptionDTO> firstLevelOptions() {
        return Arrays.stream(InterestType.values())
                .map(HubOptionDTO::from)
                .toList();
    }

    public static List<SubOptionDTO> subOptionsOf(InterestType interestType) {
        return SUB_OPTIONS.get(interestType);
    }
}