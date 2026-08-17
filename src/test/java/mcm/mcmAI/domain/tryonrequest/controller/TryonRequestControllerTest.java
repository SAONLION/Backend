package mcm.mcmAI.domain.tryonrequest.controller;

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
class TryonRequestControllerTest {

    private static final AtomicLong SKU_ID_SEQUENCE = new AtomicLong(960_000_000L);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SkuRepository skuRepository;

    @Test
    void 착장_요청이_정상적으로_생성된다() throws Exception {
        Session session = newSession();
        Sku sku = newSku(newProduct());

        String requestBody = """
                {"sku": %d, "size": "S-M", "color": "베이지"}
                """.formatted(sku.getSku());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/tryon-requests", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tryonRequestId").exists())
                .andExpect(jsonPath("$.sku").value(sku.getSku()))
                .andExpect(jsonPath("$.size").value("S-M"))
                .andExpect(jsonPath("$.color").value("베이지"))
                .andExpect(jsonPath("$.requestedAt").exists());
    }

    @Test
    void 존재하지_않는_sku면_404와_SKU_NOT_FOUND_코드를_반환한다() throws Exception {
        Session session = newSession();

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/tryon-requests", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\": 999999999, \"size\": \"S-M\", \"color\": \"베이지\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SKU_NOT_FOUND"));
    }

    @Test
    void 존재하지_않는_sessionId면_404와_SESSION_NOT_FOUND_코드를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/tryon-requests", "not-exist-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\": 1, \"size\": \"S-M\", \"color\": \"베이지\"}"))
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
                .name("테스트 상품 " + UUID.randomUUID())
                .category("bag")
                .build());
    }

    private Sku newSku(Product product) {
        return skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet())
                .product(product)
                .color("Cognac")
                .size("ONE")
                .price(200_000)
                .stockQty(5)
                .styleNumber("TSTYLE" + SKU_ID_SEQUENCE.get())
                .build());
    }
}
