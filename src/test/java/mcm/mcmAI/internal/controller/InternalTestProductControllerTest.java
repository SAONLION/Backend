package mcm.mcmAI.internal.controller;

import mcm.mcmAI.support.AbstractIntegrationTest;

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
    void 세션의_첫_스캔은_항상_FIRST_TURN_TAG_ID를_반환한다() throws Exception {
        Session session = newSession();
        Product firstTurnProduct = newProduct("bags_all");
        newSku(1L, firstTurnProduct);
        newSku(2L, newProduct("girl_wallet"));

        mockMvc.perform(get("/internal/test/products/random-tag").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.id").value(firstTurnProduct.getProductId()));
    }

    @Test
    void 이미_태그된_SKU는_후보에서_제외되고_카테고리_문자열은_대소문자_무관하게_bag_포함이면_가방으로_분류된다() throws Exception {
        Session session = newSession();
        newSku(1L, newProduct("bags_all"));
        Product bagProduct = newProduct("Men_Bags_Travel");
        newSku(2L, bagProduct);

        mockMvc.perform(get("/internal/test/products/random-tag").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk());

        // 태그1(가방)이 이미 스캔됐고 가방 카테고리의 유일한 남은 후보가 tag2뿐이므로, 가중치와 무관하게 tag2가 결정적으로 나온다.
        mockMvc.perform(get("/internal/test/products/random-tag").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.id").value(bagProduct.getProductId()));
    }

    @Test
    void wallet이_포함된_카테고리는_지갑으로_분류되어_후보가_된다() throws Exception {
        Session session = newSession();
        newSku(1L, newProduct("bags_all"));
        Product walletProduct = newProduct("girl_wallet");
        newSku(2L, walletProduct);

        mockMvc.perform(get("/internal/test/products/random-tag").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk());

        // 태그1이 이미 스캔되어 가방 후보가 비었고, 남은 지갑 후보가 tag2뿐이므로 결정적으로 tag2가 나온다.
        mockMvc.perform(get("/internal/test/products/random-tag").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.id").value(walletProduct.getProductId()));
    }

    @Test
    void bag도_wallet도_포함되지_않은_카테고리는_기타로_분류된다() throws Exception {
        Session session = newSession();
        newSku(1L, newProduct("bags_all"));
        Product otherProduct = newProduct("men_shoes");
        newSku(2L, otherProduct);

        mockMvc.perform(get("/internal/test/products/random-tag").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk());

        // 태그1이 이미 스캔되어 가방 후보가 비었고, 남은 기타 후보가 tag2뿐이므로 결정적으로 tag2가 나온다.
        mockMvc.perform(get("/internal/test/products/random-tag").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.id").value(otherProduct.getProductId()));
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
