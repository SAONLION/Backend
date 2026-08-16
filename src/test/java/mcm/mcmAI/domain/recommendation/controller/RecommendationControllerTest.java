package mcm.mcmAI.domain.recommendation.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import mcm.mcmAI.domain.tagscanlog.entity.TagScanLog;
import mcm.mcmAI.domain.tagscanlog.repository.TagScanLogRepository;
import mcm.mcmAI.global.ai.OpenAiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecommendationControllerTest {

    private static final AtomicLong SKU_ID_SEQUENCE = new AtomicLong(920_000_000L);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SkuRepository skuRepository;

    @Autowired
    private TagScanLogRepository tagScanLogRepository;

    @MockitoBean
    private OpenAiClient openAiClient;

    @Test
    void 세션이_존재하지_않으면_404와_SESSION_NOT_FOUND_코드를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/session/recommendations").param("sessionId", "no-such-session"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void 태그_스캔_이력이_없으면_빈_추천_목록을_반환하고_AI를_호출하지_않는다() throws Exception {
        Session session = newSession();

        mockMvc.perform(get("/api/v1/session/recommendations").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations").isEmpty());

        verifyNoInteractions(openAiClient);
    }

    @Test
    void AI가_유효한_이유를_반환하면_추천에_reason이_포함된다() throws Exception {
        Session session = newSession();
        Product scannedProduct = newProduct("bag");
        Sku scannedSku = newSku(scannedProduct, 200_000);
        recordScan(session, scannedSku, 1);

        Product candidate = newProduct("bag");
        newSku(candidate, 220_000);

        given(openAiClient.requestChatCompletion(anyString(), anyString(), anyBoolean()))
                .willReturn(Optional.of("""
                        {"items":[{"productId":%d,"reason":"비슷한 스타일의 컴팩트한 사이즈예요."}]}
                        """.formatted(candidate.getProductId())));

        mockMvc.perform(get("/api/v1/session/recommendations").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].productId").value(candidate.getProductId()))
                .andExpect(jsonPath("$.recommendations[0].reason").value("비슷한 스타일의 컴팩트한 사이즈예요."));
    }

    @Test
    void AI가_후보_목록에_없는_productId를_지어내면_해당_추천은_reason_없이_내려간다() throws Exception {
        Session session = newSession();
        Product scannedProduct = newProduct("bag");
        Sku scannedSku = newSku(scannedProduct, 200_000);
        recordScan(session, scannedSku, 1);

        Product candidate = newProduct("bag");
        newSku(candidate, 220_000);

        given(openAiClient.requestChatCompletion(anyString(), anyString(), anyBoolean()))
                .willReturn(Optional.of("""
                        {"items":[{"productId":999999999,"reason":"존재하지 않는 상품에 대한 이유"}]}
                        """));

        mockMvc.perform(get("/api/v1/session/recommendations").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].productId").value(candidate.getProductId()))
                .andExpect(jsonPath("$.recommendations[0].reason").value(nullValue()));
    }

    @Test
    void AI_호출이_실패해도_reason_없이_기본_추천_목록을_반환한다() throws Exception {
        Session session = newSession();
        Product scannedProduct = newProduct("bag");
        Sku scannedSku = newSku(scannedProduct, 200_000);
        recordScan(session, scannedSku, 1);

        Product candidate = newProduct("bag");
        newSku(candidate, 220_000);

        given(openAiClient.requestChatCompletion(anyString(), anyString(), anyBoolean()))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/session/recommendations").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].productId").value(candidate.getProductId()))
                .andExpect(jsonPath("$.recommendations[0].reason").value(nullValue()));
    }

    @Test
    void 카테고리나_가격대가_다르면_후보에서_제외되어_빈_목록을_반환한다() throws Exception {
        Session session = newSession();
        Product scannedProduct = newProduct("bag");
        Sku scannedSku = newSku(scannedProduct, 200_000);
        recordScan(session, scannedSku, 1);

        Product otherCategory = newProduct("shoes");
        newSku(otherCategory, 200_000);

        Product tooExpensive = newProduct("bag");
        newSku(tooExpensive, 1_000_000);

        mockMvc.perform(get("/api/v1/session/recommendations").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations").isEmpty());

        verifyNoInteractions(openAiClient);
    }

    private Session newSession() {
        return sessionRepository.save(Session.builder()
                .sessionId(UUID.randomUUID().toString())
                .language("ko")
                .build());
    }

    private Product newProduct(String category) {
        return productRepository.save(Product.builder()
                .name("테스트 상품 " + UUID.randomUUID())
                .category(category)
                .build());
    }

    private Sku newSku(Product product, int price) {
        return skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet())
                .product(product)
                .color("Black")
                .size("M")
                .price(price)
                .stockQty(5)
                .build());
    }

    private void recordScan(Session session, Sku sku, int scanOrder) {
        tagScanLogRepository.save(TagScanLog.builder()
                .session(session)
                .sku(sku)
                .scanOrder(scanOrder)
                .scannedAt(LocalDateTime.now())
                .build());
    }
}