package mcm.mcmAI.domain.product.controller;

import mcm.mcmAI.support.AbstractIntegrationTest;

import static org.hamcrest.Matchers.in;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerTest extends AbstractIntegrationTest {

    private static final AtomicLong SKU_ID_SEQUENCE = new AtomicLong(900_000_000L);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SkuRepository skuRepository;

    @Test
    void 재고가_있으면_available_true와_nextStep_NONE을_반환한다() throws Exception {
        Session session = newSession();
        Product product = newProduct();
        Sku sku = newSku(product, 5);

        mockMvc.perform(post("/api/v1/products/{productId}/pickup-check", product.getProductId())
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pickupCheckBody("매장에서 바로 수령", sku.getSku())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.nextStep").value("NONE"));
    }

    @Test
    void 재고가_없으면_BLOCKER_TRIGGERED를_반환하고_CB1_팝업이_자동_생성되어_pending_action_조회에서_확인된다() throws Exception {
        Session session = newSession();
        Product product = newProduct();
        Sku sku = newSku(product, 0);

        mockMvc.perform(post("/api/v1/products/{productId}/pickup-check", product.getProductId())
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pickupCheckBody("매장에서 바로 수령", sku.getSku())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.nextStep").value("BLOCKER_TRIGGERED"));

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(true))
                .andExpect(jsonPath("$.action.blockerType").value("CB1"))
                .andExpect(jsonPath("$.action.productId").value(product.getProductId()))
                .andExpect(jsonPath("$.action.options[0].key").value("check_other_store"))
                .andExpect(jsonPath("$.action.options[1].key").value("recommend_alt"));
    }

    @Test
    void pickupMethod가_4종에_속하지_않으면_400과_INVALID_PICKUP_METHOD_코드를_반환한다() throws Exception {
        Session session = newSession();
        Product product = newProduct();
        Sku sku = newSku(product, 5);

        mockMvc.perform(post("/api/v1/products/{productId}/pickup-check", product.getProductId())
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pickupCheckBody("드론으로 배송", sku.getSku())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PICKUP_METHOD"));
    }

    @Test
    void skuId가_해당_상품의_SKU가_아니면_404와_SKU_NOT_FOUND_코드를_반환한다() throws Exception {
        Session session = newSession();
        Product product = newProduct();
        Product otherProduct = newProduct();
        Sku skuOfOtherProduct = newSku(otherProduct, 5);

        mockMvc.perform(post("/api/v1/products/{productId}/pickup-check", product.getProductId())
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pickupCheckBody("매장에서 바로 수령", skuOfOtherProduct.getSku())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SKU_NOT_FOUND"));
    }

    @Test
    void 세션이_존재하지_않으면_404와_SESSION_NOT_FOUND_코드를_반환한다() throws Exception {
        Product product = newProduct();
        Sku sku = newSku(product, 5);

        mockMvc.perform(post("/api/v1/products/{productId}/pickup-check", product.getProductId())
                        .param("sessionId", "no-such-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pickupCheckBody("매장에서 바로 수령", sku.getSku())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void 태그_스캔_응답의_product에_스캔된_SKU의_description과_shortDescription이_노출된다() throws Exception {
        Session session = newSession();
        Product product = newProduct();
        Sku sku = skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet())
                .product(product)
                .color("Cognac")
                .size("LRG")
                .stockQty(5)
                .description("우아한 로고 락 클로저가 돋보이는 Tracy 호보 백입니다.")
                .shortDescription("로고 락 클로저와 나파 가죽 트림이 돋보이는 비세토스 호보 백")
                .build());

        mockMvc.perform(get("/api/v1/products/tags/{tagId}", sku.getSku())
                        .param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.description").value("우아한 로고 락 클로저가 돋보이는 Tracy 호보 백입니다."))
                .andExpect(jsonPath("$.product.shortDescription")
                        .value("로고 락 클로저와 나파 가죽 트림이 돋보이는 비세토스 호보 백"));
    }

    @Test
    void 태그_스캔_응답의_product는_SKU에_description이_없으면_null을_반환한다() throws Exception {
        Session session = newSession();
        Product product = newProduct();
        Sku sku = newSku(product, 5);

        mockMvc.perform(get("/api/v1/products/tags/{tagId}", sku.getSku())
                        .param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.description").doesNotExist())
                .andExpect(jsonPath("$.product.shortDescription").doesNotExist());
    }

    @Test
    void 허브옵션1_소재는_상품_SKU의_바디_트림_소재를_content로_반환한다() throws Exception {
        Product product = newProduct();
        skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet())
                .product(product)
                .color("Cognac")
                .bodyMaterial("비세토스 모노그램 캔버스")
                .trimMaterial("나파 가죽")
                .build());

        mockMvc.perform(get("/api/v1/products/{productId}/hub/options/{optionId}", product.getProductId(), "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.optionId").value("1"))
                .andExpect(jsonPath("$.content").value("바디: 비세토스 모노그램 캔버스\n트림: 나파 가죽"));
    }

    @Test
    void 허브옵션1_소재는_SKU에_소재_정보가_없으면_content가_null이다() throws Exception {
        Product product = newProduct();
        newSku(product, 5);

        mockMvc.perform(get("/api/v1/products/{productId}/hub/options/{optionId}", product.getProductId(), "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").doesNotExist());
    }

    @Test
    void 허브옵션1_소재는_안감과_제조국도_content에_포함된다() throws Exception {
        Product product = newProduct();
        skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet())
                .product(product)
                .color("Cognac")
                .bodyMaterial("비세토스 모노그램 캔버스")
                .liningCareText("소가죽 안감으로 마감되었습니다.")
                .countryOfOrigin("대한민국")
                .build());

        mockMvc.perform(get("/api/v1/products/{productId}/hub/options/{optionId}", product.getProductId(), "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(
                        "바디: 비세토스 모노그램 캔버스\n소가죽 안감으로 마감되었습니다.\n제조국: 대한민국"));
    }

    @Test
    void 허브옵션1_소재는_skuId를_지정하면_해당_SKU의_값만_반환한다() throws Exception {
        Product product = newProduct();
        Sku pink = skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet())
                .product(product)
                .color("Pink")
                .hardwareText("16K 골드 톤 하드웨어")
                .build());
        Sku cognac = skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet())
                .product(product)
                .color("Cognac")
                .hardwareText("24K 골드 톤 하드웨어")
                .build());

        mockMvc.perform(get("/api/v1/products/{productId}/hub/options/{optionId}", product.getProductId(), "1")
                        .param("skuId", pink.getSku().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("16K 골드 톤 하드웨어"));

        mockMvc.perform(get("/api/v1/products/{productId}/hub/options/{optionId}", product.getProductId(), "1")
                        .param("skuId", cognac.getSku().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("24K 골드 톤 하드웨어"));
    }

    @Test
    void 허브옵션1_소재는_skuId가_해당_상품의_SKU가_아니면_404와_SKU_NOT_FOUND_코드를_반환한다() throws Exception {
        Product product = newProduct();
        newSku(product, 5);
        Product otherProduct = newProduct();
        Sku skuOfOtherProduct = newSku(otherProduct, 5);

        mockMvc.perform(get("/api/v1/products/{productId}/hub/options/{optionId}", product.getProductId(), "1")
                        .param("skuId", skuOfOtherProduct.getSku().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SKU_NOT_FOUND"));
    }

    @Test
    void 허브옵션4_원산지는_상품_SKU의_country_of_origin을_content로_반환한다() throws Exception {
        Product product = newProduct();
        skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet())
                .product(product)
                .color("Black")
                .countryOfOrigin("대한민국")
                .build());

        mockMvc.perform(get("/api/v1/products/{productId}/hub/options/{optionId}", product.getProductId(), "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("대한민국"));
    }

    @Test
    void 허브옵션6_컬러옵션은_상품에_속한_SKU들의_color_목록을_content로_반환한다() throws Exception {
        Product product = newProduct();
        skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet()).product(product).color("Black").build());
        skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet()).product(product).color("Cognac").build());

        mockMvc.perform(get("/api/v1/products/{productId}/hub/options/{optionId}", product.getProductId(), "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Black, Cognac"));
    }

    @Test
    void category_없이_요청하면_전체_카탈로그에서_유효한_SKU를_tagId로_반환한다() throws Exception {
        Sku skuBag = newSku(newProduct("bag"), 5);
        Sku skuWallet = newSku(newProduct("wallet"), 5);

        mockMvc.perform(get("/api/v1/products/tags/random"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagId", in(
                        List.of(skuBag.getSku().intValue(), skuWallet.getSku().intValue()))));
    }

    @Test
    void category를_지정하면_해당_카테고리_SKU만_tagId로_반환된다() throws Exception {
        Sku skuBag = newSku(newProduct("bag"), 5);
        newSku(newProduct("wallet"), 5);

        mockMvc.perform(get("/api/v1/products/tags/random").param("category", "bag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagId").value(skuBag.getSku()));
    }

    @Test
    void 조건에_맞는_SKU가_없으면_tagId는_null이고_200을_반환한다() throws Exception {
        newSku(newProduct("bag"), 5);

        mockMvc.perform(get("/api/v1/products/tags/random").param("category", "no-such-category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagId").doesNotExist());
    }

    @Test
    void 카테고리_전체에_상품이_없으면_tagId는_null이고_200을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/products/tags/random"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagId").doesNotExist());
    }

    @Test
    void 허브옵션7과_8은_기존_고정_안내문을_그대로_반환한다() throws Exception {
        Product product = newProduct();
        newSku(product, 5);

        mockMvc.perform(get("/api/v1/products/{productId}/hub/options/{optionId}", product.getProductId(), "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("유사한 다른 제품과 비교 정보를 안내해드려요."));

        mockMvc.perform(get("/api/v1/products/{productId}/hub/options/{optionId}", product.getProductId(), "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("함께 매치하기 좋은 스타일링을 추천해드려요."));
    }

    private Session newSession() {
        return sessionRepository.save(Session.builder()
                .sessionId(UUID.randomUUID().toString())
                .language("ko")
                .build());
    }

    private Product newProduct() {
        return newProduct("bag");
    }

    private Product newProduct(String category) {
        return productRepository.save(Product.builder()
                .name("테스트 백팩")
                .category(category)
                .build());
    }

    private Sku newSku(Product product, int stockQty) {
        return skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet())
                .product(product)
                .color("Black")
                .size("M")
                .price(100_000)
                .stockQty(stockQty)
                .build());
    }

    private String pickupCheckBody(String pickupMethod, Long skuId) {
        return "{\"pickupMethod\":\"%s\",\"skuId\":%d}".formatted(pickupMethod, skuId);
    }
}