package mcm.mcmAI.domain.product.type;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import mcm.mcmAI.domain.product.dto.HubOptionResponse;

public final class HubOptionDetailProvider {

    private static final Map<String, HubOptionResponse> DETAILS = Map.ofEntries(
            Map.entry("1", HubOptionResponse.info(
                    "1", "소재 안내", "이 제품에 사용된 소재에 대해 안내해드려요.")),
            Map.entry("2", HubOptionResponse.info(
                    "2", "헤리티지/브랜드 스토리", "브랜드의 헤리티지와 스토리를 안내해드려요.")),
            Map.entry("3", HubOptionResponse.info(
                    "3", "관리 방법 안내", "제품을 오래 사용하실 수 있는 관리 방법을 안내해드려요.")),
            Map.entry("4", HubOptionResponse.info(
                    "4", "원산지 안내", "제품의 원산지를 안내해드려요.")),
            Map.entry("5", HubOptionResponse.info(
                    "5", "사이즈 가이드", "제품별 사이즈 정보를 안내해드려요.")),
            Map.entry("6", HubOptionResponse.info(
                    "6", "컬러 옵션 안내", "선택 가능한 컬러 옵션을 안내해드려요.")),
            Map.entry("7", HubOptionResponse.info(
                    "7", "다른 제품과 비교", "유사한 다른 제품과 비교 정보를 안내해드려요.")),
            Map.entry("8", HubOptionResponse.info(
                    "8", "스타일링 추천", "함께 매치하기 좋은 스타일링을 추천해드려요.")),
            Map.entry("9", HubOptionResponse.staffMediated(
                    "9", "가격 안내",
                    "정확한 가격은 수령 방법에 따라 달라질 수 있어 직원이 안내드려요. 먼저 수령 방법을 선택해주세요.",
                    ProductFlowNextStep.SELECT_PICKUP_METHOD,
                    List.of("호텔로 배송", "공항에서 수령", "귀국지로 배송", "매장에서 바로 수령"))),
            Map.entry("10", HubOptionResponse.info(
                    "10", "재고 확인", "현재 재고 현황은 매장 상황에 따라 달라질 수 있어요.")),
            Map.entry("11", HubOptionResponse.staffMediated(
                    "11", "할인/프로모션 안내",
                    "진행 중인 할인/프로모션은 시기에 따라 달라질 수 있어 직원이 정확히 안내드려요.",
                    ProductFlowNextStep.STAFF_CALL_SUGGESTED, null)),
            Map.entry("12", HubOptionResponse.info(
                    "12", "수령 방법 안내",
                    "호텔 배송, 공항 수령, 귀국지 배송, 매장 수령 중 선택하실 수 있어요.")),
            Map.entry("13", HubOptionResponse.staffMediated(
                    "13", "직원 호출",
                    "담당 직원을 호출해드릴게요. 잠시만 기다려주세요.",
                    ProductFlowNextStep.STAFF_CALL_SUGGESTED, null)),
            Map.entry("14", HubOptionResponse.info(
                    "14", "교환/환불 안내", "교환 및 환불 정책을 안내해드려요.")),
            Map.entry("15", HubOptionResponse.staffMediated(
                    "15", "기타 문의",
                    "문의하신 내용은 직원이 직접 안내드려요.",
                    ProductFlowNextStep.STAFF_CALL_SUGGESTED, null))
    );

    private HubOptionDetailProvider() {
    }

    public static Optional<HubOptionResponse> detailOf(String optionId) {
        return Optional.ofNullable(DETAILS.get(optionId));
    }
}