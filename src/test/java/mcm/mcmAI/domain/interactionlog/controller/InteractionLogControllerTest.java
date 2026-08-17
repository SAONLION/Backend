package mcm.mcmAI.domain.interactionlog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import mcm.mcmAI.domain.interactionlog.entity.InteractionLog;
import mcm.mcmAI.domain.interactionlog.repository.InteractionLogRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InteractionLogControllerTest {

    private static final AtomicLong SKU_ID_SEQUENCE = new AtomicLong(950_000_000L);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SkuRepository skuRepository;

    @Autowired
    private InteractionLogRepository interactionLogRepository;

    @Test
    void durationSeconds가_있으면_저장하고_응답에_포함한다() throws Exception {
        Session session = newSession();
        Sku sku = newSku(newProduct());

        String requestBody = """
                {"sku": %d, "interestType": "PURCHASE_CONDITION", "subOption": "opt_stock", "durationSeconds": 12}
                """.formatted(sku.getSku());

        MvcResult result = mockMvc.perform(post("/api/v1/sessions/{sessionId}/interactions", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value(sku.getSku()))
                .andExpect(jsonPath("$.interestType").value("PURCHASE_CONDITION"))
                .andExpect(jsonPath("$.subOption").value("opt_stock"))
                .andExpect(jsonPath("$.durationSeconds").value(12))
                .andReturn();

        Long interactionId = interactionId(result);
        InteractionLog saved = interactionLogRepository.findById(interactionId).orElseThrow();
        assertThat(saved.getDurationSeconds()).isEqualTo(12);
    }

    @Test
    void durationSeconds가_없어도_기존과_동일하게_정상_처리된다() throws Exception {
        Session session = newSession();
        Sku sku = newSku(newProduct());

        String requestBody = """
                {"sku": %d, "interestType": "PURCHASE_CONDITION", "subOption": "opt_stock"}
                """.formatted(sku.getSku());

        MvcResult result = mockMvc.perform(post("/api/v1/sessions/{sessionId}/interactions", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value(sku.getSku()))
                .andExpect(jsonPath("$.interestType").value("PURCHASE_CONDITION"))
                .andExpect(jsonPath("$.subOption").value("opt_stock"))
                .andExpect(jsonPath("$.durationSeconds").doesNotExist())
                .andReturn();

        Long interactionId = interactionId(result);
        InteractionLog saved = interactionLogRepository.findById(interactionId).orElseThrow();
        assertThat(saved.getDurationSeconds()).isNull();
    }

    private Long interactionId(MvcResult result) throws Exception {
        Number interactionId = JsonPath.read(result.getResponse().getContentAsString(), "$.interactionId");
        return interactionId.longValue();
    }

    private Session newSession() {
        return sessionRepository.saveAndFlush(Session.builder()
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
