package mcm.mcmAI.domain.product.type;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import mcm.mcmAI.domain.product.dto.HubOptionDTO;
import mcm.mcmAI.domain.product.dto.HubOptionResponse;
import mcm.mcmAI.domain.product.dto.SubOptionDTO;


public final class HubOptionProvider {

    private static final Map<InterestType, List<SubOptionDTO>> SUB_OPTIONS = Map.of(
            InterestType.PRODUCT_UNDERSTANDING, List.of(
                    new SubOptionDTO(1L, "소재 안내", "MATERIAL", mediationOf("1")),
                    new SubOptionDTO(2L, "헤리티지/브랜드 스토리", "HERITAGE", mediationOf("2")),
                    new SubOptionDTO(3L, "관리 방법", "CARE", mediationOf("3")),
                    new SubOptionDTO(4L, "원산지", "ORIGIN", mediationOf("4"))
            ),
            InterestType.FIT_PREFERENCE, List.of(
                    new SubOptionDTO(5L, "사이즈 가이드", "SIZE_GUIDE", mediationOf("5")),
                    new SubOptionDTO(6L, "컬러 옵션", "COLOR_OPTION", mediationOf("6")),
                    new SubOptionDTO(7L, "다른 제품과 비교", "COMPARE", mediationOf("7")),
                    new SubOptionDTO(8L, "스타일링 추천", "STYLING", mediationOf("8"))
            ),
            InterestType.PURCHASE_CONDITION, List.of(
                    new SubOptionDTO(9L, "가격 안내", "PRICE", mediationOf("9")),
                    new SubOptionDTO(10L, "재고 확인", "STOCK", mediationOf("10")),
                    new SubOptionDTO(11L, "할인/프로모션", "PROMOTION", mediationOf("11")),
                    new SubOptionDTO(12L, "매장 픽업", "PICKUP", mediationOf("12"))
            ),
            InterestType.OTHER, List.of(
                    new SubOptionDTO(13L, "직원 호출", "CALL_STAFF", mediationOf("13")),
                    new SubOptionDTO(14L, "교환/환불 문의", "EXCHANGE_REFUND", mediationOf("14")),
                    new SubOptionDTO(15L, "기타 문의", "ETC", mediationOf("15"))
            )
    );

    private HubOptionProvider() {
    }

    private static String mediationOf(String optionId) {
        return HubOptionDetailProvider.detailOf(optionId)
                .map(HubOptionResponse::type)
                .orElse(null);
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