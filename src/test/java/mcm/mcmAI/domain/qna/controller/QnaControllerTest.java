package mcm.mcmAI.domain.qna.controller;

import mcm.mcmAI.support.AbstractIntegrationTest;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.qna.type.QnaFixedAnswers;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.global.ai.OpenAiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QnaControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private OpenAiClient openAiClient;

    @Test
    void AS_REPAIR은_고정_답변을_AI_호출_없이_반환한다() throws Exception {
        assertFixedAnswer("AS_REPAIR",
                "A/S 및 수선 상담을 받으실 땐, 보증서와 구매 영수증을 지참해주시면 더 빠르고 정확하게 안내받으실 수 있어요. "
                        + "보증서가 없으신 경우, MCM 공식 고객센터(1600-1976) 또는 이메일(contact.kr@mcmworldwide.com)로 "
                        + "문의해주시면 확인 도와드려요. 지금 매장에 계시다면, 직원 호출 버튼을 이용해주셔도 좋아요.");
    }

    @Test
    void CARE는_고정_답변을_AI_호출_없이_반환한다() throws Exception {
        assertFixedAnswer("CARE",
                "MCM 제품은 오래 쓸 수 있도록 정교하게 만들어졌지만, 평소 관리도 중요해요. 사용하지 않을 땐 제공된 더스트백에 "
                        + "넣어 직사광선을 피해 서늘하고 건조한 곳에 보관해주세요. 표면이 젖거나 오염됐을 땐, 보풀 없는 밝은 색 "
                        + "천으로 가볍게 닦아 말려주시고, 비누나 솔벤트는 사용하지 마세요. 드라이클리닝, 표백, 건조기 사용은 "
                        + "피해주시고, 거친 표면과의 마찰도 주의해주세요. 적절히 관리하시면 시간이 지나며 더 멋스러운 색감을 낼 "
                        + "수 있어요. 더 궁금하신 점은 매장 직원에게 문의해주세요.");
    }

    @Test
    void GIFT_WRAP은_고정_답변을_AI_호출_없이_반환한다() throws Exception {
        assertFixedAnswer("GIFT_WRAP",
                "MCM은 지속가능한 가치를 위해, 별도의 선물 포장 대신 시그니처 쇼핑백으로 제품을 담아드리고 있어요. 결제 완료 "
                        + "후에는 CJ대한통운을 통해 영업일 기준 1~2일 이내에 배송이 시작되고, MCM.com 모든 주문에는 무료 "
                        + "배송이 기본으로 제공돼요. 더 궁금하신 점은 매장 직원에게 문의해주세요.");
    }

    @Test
    void TAX_REFUND는_고정_답변을_AI_호출_없이_반환한다() throws Exception {
        assertFixedAnswer("TAX_REFUND",
                "외국인 관광객이시라면 일정 금액 이상 구매 시 면세 혜택을 받으실 수 있어요. 결제 시 여권을 제시해주시면 면세 "
                        + "서류를 준비해드리고, 출국 시 공항 면세환급 창구에서 환급받으실 수 있어요. 정확한 조건과 절차는 매장 "
                        + "직원이 자세히 안내해드려요.");
    }

    @Test
    void SHIPPING_RETURN은_세션_language에_따라_다른_답변을_반환한다() throws Exception {
        assertShippingReturnAnswer("ko", QnaFixedAnswers.shippingReturnAnswer("ko"));
        assertShippingReturnAnswer("zh", QnaFixedAnswers.shippingReturnAnswer("zh"));
        assertShippingReturnAnswer("en", QnaFixedAnswers.shippingReturnAnswer("en"));
        assertShippingReturnAnswer("ja", QnaFixedAnswers.shippingReturnAnswer("ja"));
    }

    @Test
    void SHIPPING_RETURN은_지원하지_않는_language면_한국어_기본값을_반환한다() throws Exception {
        assertShippingReturnAnswer("fr", QnaFixedAnswers.shippingReturnAnswer("ko"));
    }

    @Test
    void FREE_TEXT는_AI를_호출해서_답변을_반환한다() throws Exception {
        Session session = newSession("ko");
        Product product = newProduct();

        given(openAiClient.requestChatCompletion(anyString(), anyString(), anyBoolean()))
                .willReturn(Optional.of("비 오는 날에도 편하게 사용하실 수 있는 소재예요."));

        String requestBody = """
                {"questionType": "FREE_TEXT", "question": "비 오는 날 들어도 괜찮을까요?"}
                """;

        mockMvc.perform(post("/api/v1/products/{productId}/qna", product.getProductId())
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("비 오는 날에도 편하게 사용하실 수 있는 소재예요."));
    }

    @Test
    void FREE_TEXT에서_AI_호출이_실패하면_language별_fallback_답변을_반환한다() throws Exception {
        Session koSession = newSession("ko");
        Product product = newProduct();

        given(openAiClient.requestChatCompletion(anyString(), anyString(), anyBoolean()))
                .willReturn(Optional.empty());

        String requestBody = """
                {"questionType": "FREE_TEXT", "question": "이 가방 방수 되나요?"}
                """;

        mockMvc.perform(post("/api/v1/products/{productId}/qna", product.getProductId())
                        .param("sessionId", koSession.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(QnaFixedAnswers.freeTextFallbackAnswer("ko")));

        Session enSession = newSession("en");

        mockMvc.perform(post("/api/v1/products/{productId}/qna", product.getProductId())
                        .param("sessionId", enSession.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(QnaFixedAnswers.freeTextFallbackAnswer("en")));
    }

    @Test
    void 존재하지_않는_productId면_404와_PRODUCT_NOT_FOUND_코드를_반환한다() throws Exception {
        Session session = newSession("ko");

        mockMvc.perform(post("/api/v1/products/{productId}/qna", 999_999_999L)
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionType\": \"CARE\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

        verifyNoInteractions(openAiClient);
    }

    @Test
    void 존재하지_않는_sessionId면_404와_SESSION_NOT_FOUND_코드를_반환한다() throws Exception {
        Product product = newProduct();

        mockMvc.perform(post("/api/v1/products/{productId}/qna", product.getProductId())
                        .param("sessionId", "not-exist-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionType\": \"CARE\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void FREE_TEXT인데_question이_없으면_400과_MISSING_QNA_QUESTION_코드를_반환한다() throws Exception {
        Session session = newSession("ko");
        Product product = newProduct();

        mockMvc.perform(post("/api/v1/products/{productId}/qna", product.getProductId())
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionType\": \"FREE_TEXT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_QNA_QUESTION"));

        verifyNoInteractions(openAiClient);
    }

    @Test
    void 허용되지_않는_questionType이면_400과_INVALID_QUESTION_TYPE_코드를_반환한다() throws Exception {
        Session session = newSession("ko");
        Product product = newProduct();

        mockMvc.perform(post("/api/v1/products/{productId}/qna", product.getProductId())
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionType\": \"UNKNOWN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_QUESTION_TYPE"));
    }

    private void assertFixedAnswer(String questionType, String expectedAnswer) throws Exception {
        Session session = newSession("ko");
        Product product = newProduct();

        mockMvc.perform(post("/api/v1/products/{productId}/qna", product.getProductId())
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionType\": \"%s\"}".formatted(questionType)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(expectedAnswer));

        verifyNoInteractions(openAiClient);
    }

    private void assertShippingReturnAnswer(String language, String expectedAnswer) throws Exception {
        Session session = newSession(language);
        Product product = newProduct();

        mockMvc.perform(post("/api/v1/products/{productId}/qna", product.getProductId())
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionType\": \"SHIPPING_RETURN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(expectedAnswer));

        verifyNoInteractions(openAiClient);
    }

    private Session newSession(String language) {
        return sessionRepository.save(Session.builder()
                .sessionId(UUID.randomUUID().toString())
                .language(language)
                .build());
    }

    private Product newProduct() {
        return productRepository.save(Product.builder()
                .name("테스트 상품 " + UUID.randomUUID())
                .category("bag")
                .materialDesc("최고급 가죽 소재")
                .heritageDesc("MCM 헤리티지 라인")
                .build());
    }
}
