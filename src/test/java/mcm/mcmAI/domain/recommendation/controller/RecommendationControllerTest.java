package mcm.mcmAI.domain.recommendation.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import mcm.mcmAI.domain.skuimage.entity.SkuImage;
import mcm.mcmAI.domain.skuimage.repository.SkuImageRepository;
import mcm.mcmAI.domain.skuimage.type.ShotType;
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

    @Autowired
    private SkuImageRepository skuImageRepository;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @MockitoBean
    private OpenAiClient openAiClient;

    @Test
    void 세션이_존재하지_않으면_404와_SESSION_NOT_FOUND_코드를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/session/recommendations").param("sessionId", "no-such-session"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void 태그_스캔_이력이_없고_채울_후보도_없으면_NO_CANDIDATE_상태와_빈_목록을_반환하고_AI를_호출하지_않는다() throws Exception {
        Session session = newSession();

        mockMvc.perform(get("/api/v1/session/recommendations").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_CANDIDATE"))
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations").isEmpty());

        verifyNoInteractions(openAiClient);
    }

    @Test
    void 태그_스캔_이력이_없어도_인기_상품이_있으면_그것으로_채워_OK_상태를_반환하고_AI를_호출하지_않는다() throws Exception {
        Session session = newSession();
        Product popular = newProduct("bag");
        newSku(popular, 200_000);

        mockMvc.perform(get("/api/v1/session/recommendations").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.recommendations.length()").value(1))
                .andExpect(jsonPath("$.recommendations[0].productId").value(popular.getProductId()))
                .andExpect(jsonPath("$.recommendations[0].reason").value(nullValue()));

        verifyNoInteractions(openAiClient);
    }

    @Test
    void 유사도가_높은_상위_3개_후보가_이유와_함께_추천된다() throws Exception {
        Session session = newSession();
        Product scannedProduct = newVectorizedProduct("bag", new float[]{1f, 0f, 0f});
        recordScan(session, newSku(scannedProduct, 200_000), 1);

        Product best = newVectorizedProduct("bag", new float[]{1f, 0f, 0f});
        Product good = newVectorizedProduct("bag", new float[]{0.8f, 0.6f, 0f});
        Product ok = newVectorizedProduct("bag", new float[]{0.6f, 0.8f, 0f});
        Product far = newVectorizedProduct("bag", new float[]{0f, 0f, 1f});

        given(openAiClient.requestEmbedding(anyString())).willReturn(Optional.of(new float[]{1f, 0f, 0f}));
        given(openAiClient.requestChatCompletion(anyString(), anyString(), anyBoolean()))
                .willReturn(Optional.of("""
                        {"items":[
                          {"productId":%d,"reason":"가장 잘 어울려요."},
                          {"productId":%d,"reason":"비슷한 스타일이에요."},
                          {"productId":%d,"reason":"취향에 맞을 거예요."}
                        ]}
                        """.formatted(best.getProductId(), good.getProductId(), ok.getProductId())));

        mockMvc.perform(get("/api/v1/session/recommendations").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations.length()").value(3))
                .andExpect(jsonPath("$.recommendations[0].productId").value(best.getProductId()))
                .andExpect(jsonPath("$.recommendations[0].reason").value("가장 잘 어울려요."))
                .andExpect(jsonPath("$.recommendations[1].productId").value(good.getProductId()))
                .andExpect(jsonPath("$.recommendations[2].productId").value(ok.getProductId()));
    }

    @Test
    void 이미_태그한_제품은_유사도가_가장_높아도_후보에서_제외된다() throws Exception {
        Session session = newSession();
        Product scannedProduct = newVectorizedProduct("bag", new float[]{1f, 0f, 0f});
        recordScan(session, newSku(scannedProduct, 200_000), 1);

        Product untaggedCandidate = newVectorizedProduct("bag", new float[]{0.5f, 0.5f, 0f});

        given(openAiClient.requestEmbedding(anyString())).willReturn(Optional.of(new float[]{1f, 0f, 0f}));
        given(openAiClient.requestChatCompletion(anyString(), anyString(), anyBoolean()))
                .willReturn(Optional.of("""
                        {"items":[{"productId":%d,"reason":"관심 있으실 만한 상품이에요."}]}
                        """.formatted(untaggedCandidate.getProductId())));

        mockMvc.perform(get("/api/v1/session/recommendations").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations.length()").value(1))
                .andExpect(jsonPath("$.recommendations[0].productId").value(untaggedCandidate.getProductId()));
    }

    @Test
    void AI가_후보_목록에_없는_productId를_지어내면_해당_추천은_이유_없이_내려간다() throws Exception {
        Session session = newSession();
        Product scannedProduct = newVectorizedProduct("bag", new float[]{1f, 0f, 0f});
        recordScan(session, newSku(scannedProduct, 200_000), 1);

        Product candidate = newVectorizedProduct("bag", new float[]{1f, 0f, 0f});

        given(openAiClient.requestEmbedding(anyString())).willReturn(Optional.of(new float[]{1f, 0f, 0f}));
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
    void 임베딩_생성에_실패하면_카테고리_가격대_규칙으로_폴백하고_이유_없이_반환한다() throws Exception {
        Session session = newSession();
        Product scannedProduct = newProduct("bag");
        recordScan(session, newSku(scannedProduct, 200_000), 1);

        Product fallbackCandidate = newProduct("bag");
        newSku(fallbackCandidate, 220_000);

        given(openAiClient.requestEmbedding(anyString())).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/session/recommendations").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].productId").value(fallbackCandidate.getProductId()))
                .andExpect(jsonPath("$.recommendations[0].reason").value(nullValue()));

        verify(openAiClient, never()).requestChatCompletion(anyString(), anyString(), anyBoolean());
    }

    @Test
    void 이유_생성_호출이_실패하면_AI_후보를_버리고_카테고리_가격대_규칙과_인기_후보로_폴백한다() throws Exception {
        Session session = newSession();
        Product scannedProduct = newVectorizedProduct("bag", new float[]{1f, 0f, 0f});
        recordScan(session, newSku(scannedProduct, 200_000), 1);

        // 임베딩 유사도로는 후보가 되지만, 규칙 기반 폴백 조건(카테고리/가격대)은 만족하지 않는 상품
        // -> AI 이유 생성이 실패하면 이 상품은 신뢰하지 않고 버리되, 규칙/인기 후보로도 채울 게 없으면 최후 인기 폴백으로 채워진다.
        Product looseFitProduct = newVectorizedProduct("shoes", new float[]{0.9f, 0.1f, 0f});

        Product fallbackCandidate = newProduct("bag");
        newSku(fallbackCandidate, 220_000);

        given(openAiClient.requestEmbedding(anyString())).willReturn(Optional.of(new float[]{1f, 0f, 0f}));
        given(openAiClient.requestChatCompletion(anyString(), anyString(), anyBoolean()))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/session/recommendations").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.recommendations.length()").value(2))
                .andExpect(jsonPath("$.recommendations[0].productId").value(fallbackCandidate.getProductId()))
                .andExpect(jsonPath("$.recommendations[0].reason").value(nullValue()))
                .andExpect(jsonPath("$.recommendations[1].productId").value(looseFitProduct.getProductId()))
                .andExpect(jsonPath("$.recommendations[1].reason").value(nullValue()));
    }

    @Test
    void 카테고리와_가격대_규칙만으로_3개를_채우지_못하면_규칙을_완화하고_인기_후보로_채운다() throws Exception {
        Session session = newSession();
        Product scannedProduct = newProduct("bag");
        recordScan(session, newSku(scannedProduct, 200_000), 1);

        Product otherCategory = newProduct("shoes");
        newSku(otherCategory, 200_000);

        Product tooExpensive = newProduct("bag");
        newSku(tooExpensive, 1_000_000);

        given(openAiClient.requestEmbedding(anyString())).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/session/recommendations").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                // 1단계(카테고리+가격대 ±30%)는 0건 -> 2단계(카테고리만, 가격대 무시)로 tooExpensive 채택
                .andExpect(jsonPath("$.recommendations[0].productId").value(tooExpensive.getProductId()))
                .andExpect(jsonPath("$.recommendations[0].reason").value(nullValue()))
                // 그래도 3개가 안 되므로 3단계(전체 인기 상품, 카테고리 무관)로 otherCategory까지 채택
                .andExpect(jsonPath("$.recommendations[1].productId").value(otherCategory.getProductId()))
                .andExpect(jsonPath("$.recommendations[1].reason").value(nullValue()))
                .andExpect(jsonPath("$.recommendations.length()").value(2));

        verify(openAiClient, never()).requestChatCompletion(anyString(), anyString(), anyBoolean());
    }

    @Test
    void 추천_결과에_대표_SKU_ID와_이미지_URL이_포함된다() throws Exception {
        Session session = newSession();
        Product scannedProduct = newVectorizedProduct("bag", new float[]{1f, 0f, 0f});
        recordScan(session, newSku(scannedProduct, 200_000), 1);

        Product candidate = newVectorizedProduct("bag", new float[]{1f, 0f, 0f});
        Sku candidateSku = newSku(candidate, 210_000, "STYLE-001");
        skuImageRepository.save(SkuImage.builder()
                .styleNumber("STYLE-001")
                .position(1)
                .imageUrl("https://cdn.example.com/style-001.jpg")
                .shotType(ShotType.PRODUCT)
                .hasPerson(false)
                .build());

        given(openAiClient.requestEmbedding(anyString())).willReturn(Optional.of(new float[]{1f, 0f, 0f}));
        given(openAiClient.requestChatCompletion(anyString(), anyString(), anyBoolean()))
                .willReturn(Optional.of("""
                        {"items":[{"productId":%d,"reason":"잘 어울려요."}]}
                        """.formatted(candidate.getProductId())));

        mockMvc.perform(get("/api/v1/session/recommendations").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].productId").value(candidate.getProductId()))
                .andExpect(jsonPath("$.recommendations[0].skuId").value(candidateSku.getSku()))
                .andExpect(jsonPath("$.recommendations[0].imageUrl")
                        .value("https://cdn.example.com/style-001.jpg"));
    }

    @Test
    void 태그_이력_개수와_무관하게_충분한_후보가_있으면_추천은_항상_3개다() throws Exception {
        given(openAiClient.requestEmbedding(anyString())).willReturn(Optional.empty());

        // 최대 태그 이력(8개) + 최소 필요 후보(3개)를 감안해 여유 있게 풀을 준비한다.
        List<Sku> pool = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            pool.add(newSku(newProduct("bag"), 200_000));
        }

        for (int tagCount = 1; tagCount <= 8; tagCount++) {
            Session session = newSession();
            for (int i = 0; i < tagCount; i++) {
                recordScan(session, pool.get(i), i + 1);
            }

            mockMvc.perform(get("/api/v1/session/recommendations").param("sessionId", session.getSessionId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("OK"))
                    .andExpect(jsonPath("$.recommendations.length()").value(3));
        }
    }

    @Test
    void 최대_3개까지만_반환된다() throws Exception {
        Session session = newSession();
        Product scannedProduct = newVectorizedProduct("bag", new float[]{1f, 0f, 0f});
        recordScan(session, newSku(scannedProduct, 200_000), 1);

        for (int i = 0; i < 5; i++) {
            newVectorizedProduct("bag", new float[]{1f - i * 0.05f, i * 0.05f, 0f});
        }

        given(openAiClient.requestEmbedding(anyString())).willReturn(Optional.of(new float[]{1f, 0f, 0f}));
        given(openAiClient.requestChatCompletion(anyString(), anyString(), anyBoolean()))
                .willReturn(Optional.of("""
                        {"items":[]}
                        """));

        mockMvc.perform(get("/api/v1/session/recommendations").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations.length()").value(3));
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

    private Product newVectorizedProduct(String category, float[] embedding) {
        Product product = newProduct(category);
        product.updateEmbedding(encodeEmbedding(embedding));
        return productRepository.save(product);
    }

    private String encodeEmbedding(float[] embedding) {
        try {
            return OBJECT_MAPPER.writeValueAsString(embedding);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private Sku newSku(Product product, int price) {
        return newSku(product, price, null);
    }

    private Sku newSku(Product product, int price, String styleNumber) {
        return skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet())
                .product(product)
                .color("Black")
                .size("M")
                .price(price)
                .stockQty(5)
                .styleNumber(styleNumber)
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
