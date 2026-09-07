package mcm.mcmAI.internal.controller;

import mcm.mcmAI.support.AbstractIntegrationTest;

import com.jayway.jsonpath.JsonPath;

import static org.hamcrest.Matchers.in;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.internal-test-endpoints.enabled=true")
class InternalTestProductControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SkuRepository skuRepository;

    @Test
    void 세션의_첫_스캔도_카탈로그_전체_중_무작위로_반환한다() throws Exception {
        Session session = newSession();
        Product product1 = newProduct("bags_all");
        newSku(1L, product1);
        Product product2 = newProduct("girl_wallet");
        newSku(2L, product2);

        mockMvc.perform(get("/internal/test/products/random-tag").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.id", in(
                        List.of(product1.getProductId().intValue(), product2.getProductId().intValue()))));
    }

    @Test
    void 이미_태그된_SKU는_카테고리와_무관하게_후보에서_제외된다() throws Exception {
        Session session = newSession();
        Product product1 = newProduct("bags_all");
        newSku(1L, product1);
        Product product2 = newProduct("men_shoes");
        newSku(2L, product2);

        MvcResult firstResult = mockMvc.perform(
                        get("/internal/test/products/random-tag").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andReturn();

        long firstProductId = JsonPath.read(firstResult.getResponse().getContentAsString(), "$.product.id");
        long remainingProductId = firstProductId == product1.getProductId() ? product2.getProductId() : product1.getProductId();

        // 첫 호출에서 나온 SKU가 이미 스캔됐으므로, 카테고리 구분 없이 전체에서 남은 후보가 하나뿐이라 결정적으로 나온다.
        mockMvc.perform(get("/internal/test/products/random-tag").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.id").value(remainingProductId));
    }

    @Test
    void 카탈로그_전체를_다_태그하면_제외_없이_처음부터_다시_순환한다() throws Exception {
        Session session = newSession();
        Product product1 = newProduct("bags_all");
        newSku(1L, product1);
        Product product2 = newProduct("bags_all");
        newSku(2L, product2);

        mockMvc.perform(get("/internal/test/products/random-tag").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/internal/test/products/random-tag").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk());

        // tag1, tag2 모두 태그된 상태 -> 후보가 바닥나 제외 없이 처음부터 다시 순환하므로 둘 중 하나가 나온다.
        mockMvc.perform(get("/internal/test/products/random-tag").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.id", in(
                        List.of(product1.getProductId().intValue(), product2.getProductId().intValue()))));
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

    private Sku newSku(Long skuId, Product product) {
        return skuRepository.save(Sku.builder()
                .sku(skuId)
                .product(product)
                .color("Black")
                .size("ONE")
                .price(100_000)
                .stockQty(5)
                .build());
    }
}
