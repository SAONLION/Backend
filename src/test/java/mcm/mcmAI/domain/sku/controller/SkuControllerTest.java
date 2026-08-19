package mcm.mcmAI.domain.sku.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicLong;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import mcm.mcmAI.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SkuControllerTest extends AbstractIntegrationTest {

    private static final AtomicLong SKU_ID_SEQUENCE = new AtomicLong(910_000_000L);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SkuRepository skuRepository;

    @Test
    void SKU_상세_응답에_dimensions_storage_strap이_노출된다() throws Exception {
        Product product = newProduct();
        Sku sku = skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet())
                .product(product)
                .color("Cognac")
                .size("LRG")
                .dimensionsText("약 11 x 33 x 31 센티미터")
                .storageText("내부 슬립 포켓 및 지퍼 수납공간")
                .strapLength("73cm - 105cm")
                .handleDrop("27 cm")
                .build());

        mockMvc.perform(get("/api/v1/products/{productId}/skus/{skuId}", product.getProductId(), sku.getSku()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dimensions").value("약 11 x 33 x 31 센티미터"))
                .andExpect(jsonPath("$.storage").value("내부 슬립 포켓 및 지퍼 수납공간"))
                .andExpect(jsonPath("$.strap").value("스트랩 길이 73cm - 105cm · 핸들 드롭 27 cm"));
    }

    @Test
    void 스트랩_길이만_있으면_핸들_드롭_없이_스트랩_길이만_노출된다() throws Exception {
        Product product = newProduct();
        Sku sku = skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet())
                .product(product)
                .color("Black")
                .strapLength("109cm - 134cm")
                .build());

        mockMvc.perform(get("/api/v1/products/{productId}/skus/{skuId}", product.getProductId(), sku.getSku()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strap").value("스트랩 길이 109cm - 134cm"));
    }

    @Test
    void 값이_없는_필드는_null로_생략되고_빈문자열이_아니다() throws Exception {
        Product product = newProduct();
        Sku sku = skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet())
                .product(product)
                .color("Black")
                .build());

        mockMvc.perform(get("/api/v1/products/{productId}/skus/{skuId}", product.getProductId(), sku.getSku()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dimensions").doesNotExist())
                .andExpect(jsonPath("$.storage").doesNotExist())
                .andExpect(jsonPath("$.strap").doesNotExist());
    }

    private Product newProduct() {
        return productRepository.save(Product.builder()
                .name("테스트 백팩")
                .category("bag")
                .build());
    }
}
