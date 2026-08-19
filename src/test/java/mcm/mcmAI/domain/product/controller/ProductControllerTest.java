package mcm.mcmAI.domain.product.controller;

import mcm.mcmAI.support.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private Session newSession() {
        return sessionRepository.save(Session.builder()
                .sessionId(UUID.randomUUID().toString())
                .language("ko")
                .build());
    }

    private Product newProduct() {
        return productRepository.save(Product.builder()
                .name("테스트 백팩")
                .category("bag")
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