package mcm.mcmAI.domain.qna.type;

import java.util.Map;

/**
 * QnA 고정 답변 문구 모음. 문구만 수정하면 되도록 로직과 분리해둔다.
 */
public final class QnaFixedAnswers {

    public static final String DEFAULT_LANGUAGE = "ko";

    private static final Map<QuestionType, String> FIXED_ANSWERS = Map.of(
            QuestionType.AS_REPAIR,
            "A/S 및 수선 상담을 받으실 땐, 보증서와 구매 영수증을 지참해주시면 더 빠르고 정확하게 안내받으실 수 있어요. "
                    + "보증서가 없으신 경우, MCM 공식 고객센터(1600-1976) 또는 이메일(contact.kr@mcmworldwide.com)로 "
                    + "문의해주시면 확인 도와드려요. 지금 매장에 계시다면, 직원 호출 버튼을 이용해주셔도 좋아요.",

            QuestionType.CARE,
            "MCM 제품은 오래 쓸 수 있도록 정교하게 만들어졌지만, 평소 관리도 중요해요. 사용하지 않을 땐 제공된 더스트백에 "
                    + "넣어 직사광선을 피해 서늘하고 건조한 곳에 보관해주세요. 표면이 젖거나 오염됐을 땐, 보풀 없는 밝은 색 "
                    + "천으로 가볍게 닦아 말려주시고, 비누나 솔벤트는 사용하지 마세요. 드라이클리닝, 표백, 건조기 사용은 "
                    + "피해주시고, 거친 표면과의 마찰도 주의해주세요. 적절히 관리하시면 시간이 지나며 더 멋스러운 색감을 낼 "
                    + "수 있어요. 더 궁금하신 점은 매장 직원에게 문의해주세요.",

            QuestionType.GIFT_WRAP,
            "MCM은 지속가능한 가치를 위해, 별도의 선물 포장 대신 시그니처 쇼핑백으로 제품을 담아드리고 있어요. 결제 완료 "
                    + "후에는 CJ대한통운을 통해 영업일 기준 1~2일 이내에 배송이 시작되고, MCM.com 모든 주문에는 무료 "
                    + "배송이 기본으로 제공돼요. 더 궁금하신 점은 매장 직원에게 문의해주세요.",

            QuestionType.TAX_REFUND,
            "외국인 관광객이시라면 일정 금액 이상 구매 시 면세 혜택을 받으실 수 있어요. 결제 시 여권을 제시해주시면 면세 "
                    + "서류를 준비해드리고, 출국 시 공항 면세환급 창구에서 환급받으실 수 있어요. 정확한 조건과 절차는 매장 "
                    + "직원이 자세히 안내해드려요."
    );

    private static final Map<String, String> SHIPPING_RETURN_ANSWERS = Map.of(
            "ko",
            "결제 완료 후 CJ대한통운을 통해 영업일 기준 1~2일 이내에 배송이 시작돼요. MCM.com 모든 주문에는 무료 배송이 "
                    + "기본 제공돼요. 반품/교환 등 자세한 사항은 매장 직원에게 문의해주세요.",

            "zh",
            "표준 배송은 무료로 제공되며, 중국 본토 지역에 한해 배송 가능해요. 반품을 원하시면 고객서비스에 반품 신청을 "
                    + "해주셔야 하고, 상품은 원래 상태로 태그와 부속품을 모두 포함해 배송받은 날로부터 7일 이내에 신청해주셔야 "
                    + "해요. MCM 중국 공식 홈페이지에서 구매하신 상품만 반품이 가능해요.",

            "en",
            "Standard shipping is free within the continental U.S. (UPS/FedEx). Express shipping is available for "
                    + "$25 (2-day) or $35 (next day). Items must be returned in original condition within 30 days of "
                    + "delivery. Exchanges are not available, and final sale or 50%+ discounted items cannot be returned.",

            "ja",
            "표준 배송은 무료로 제공되며, 일본 국내에 한해 배송 가능해요. 반품에 대한 자세한 내용은 반품 정책 페이지를 "
                    + "참고해주시거나, 매장 직원에게 문의해주세요."
    );

    private static final Map<String, String> FREE_TEXT_FALLBACK_ANSWERS = Map.of(
            "ko", "지금은 제가 바로 답변드리기보다, 직원분께 여쭤보시는 게 더 정확할 것 같아요. 직원 호출 버튼을 이용해보세요!",
            "en", "I think a staff member could give you a more accurate answer right now. "
                    + "Feel free to use the staff call button!"
    );

    private QnaFixedAnswers() {
    }

    public static String fixedAnswer(QuestionType questionType) {
        String answer = FIXED_ANSWERS.get(questionType);
        if (answer == null) {
            throw new IllegalArgumentException("고정 답변이 없는 questionType 입니다: " + questionType);
        }
        return answer;
    }

    public static String shippingReturnAnswer(String language) {
        return SHIPPING_RETURN_ANSWERS.getOrDefault(language, SHIPPING_RETURN_ANSWERS.get(DEFAULT_LANGUAGE));
    }

    public static String freeTextFallbackAnswer(String language) {
        return FREE_TEXT_FALLBACK_ANSWERS.getOrDefault(language, FREE_TEXT_FALLBACK_ANSWERS.get(DEFAULT_LANGUAGE));
    }
}
