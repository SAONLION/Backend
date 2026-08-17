package mcm.mcmAI.domain.journeycard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JourneyCardControllerTest {

    private static final AtomicLong SKU_ID_SEQUENCE = new AtomicLong(940_000_000L);
    private static final AtomicLong STYLE_NUMBER_SEQUENCE = new AtomicLong(1L);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yy/MM/dd");

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

    @Test
    void 세션이_존재하지_않으면_404와_SESSION_NOT_FOUND_코드를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", "no-such-session"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void 태그_이력이_없으면_빈_콜라주를_반환하고_기본_필드는_정상적으로_채워진다() throws Exception {
        Session session = newSession("mingyu");

        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand").value("MCM"))
                .andExpect(jsonPath("$.date").value(session.getCreatedAt().format(DATE_FORMATTER)))
                .andExpect(jsonPath("$.nickname").value("mingyu"))
                .andExpect(jsonPath("$.sessionCode").value(session.getSessionId().substring(0, 5)))
                .andExpect(jsonPath("$.collageImages").isArray())
                .andExpect(jsonPath("$.collageImages").isEmpty());
    }

    @Test
    void 태그한_제품에_모델샷이_있으면_모델샷_1장과_제품샷_3장으로_채운다() throws Exception {
        Session session = newSession("mingyu");
        String styleNumber = newStyleNumber();
        Product product = newProduct();
        Sku sku = newSku(product, styleNumber);
        recordScan(session, sku, 1);

        String model1 = newSkuImage(styleNumber, 1, ShotType.MODEL);
        newSkuImage(styleNumber, 2, ShotType.MODEL);
        String product1 = newSkuImage(styleNumber, 3, ShotType.PRODUCT);
        String product2 = newSkuImage(styleNumber, 4, ShotType.PRODUCT);
        String product3 = newSkuImage(styleNumber, 5, ShotType.PRODUCT);

        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collageImages.length()").value(4))
                .andExpect(jsonPath("$.collageImages[0].imageUrl").value(model1))
                .andExpect(jsonPath("$.collageImages[0].shotType").value("MODEL"))
                .andExpect(jsonPath("$.collageImages[1].imageUrl").value(product1))
                .andExpect(jsonPath("$.collageImages[1].shotType").value("PRODUCT"))
                .andExpect(jsonPath("$.collageImages[2].imageUrl").value(product2))
                .andExpect(jsonPath("$.collageImages[3].imageUrl").value(product3));
    }

    @Test
    void 태그한_제품에_모델샷이_없으면_제품샷만으로_채운다() throws Exception {
        Session session = newSession("mingyu");
        String styleNumber = newStyleNumber();
        Product product = newProduct();
        Sku sku = newSku(product, styleNumber);
        recordScan(session, sku, 1);

        for (int position = 1; position <= 5; position++) {
            newSkuImage(styleNumber, position, ShotType.PRODUCT);
        }

        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collageImages.length()").value(4))
                .andExpect(jsonPath("$.collageImages[0].shotType").value("PRODUCT"))
                .andExpect(jsonPath("$.collageImages[1].shotType").value("PRODUCT"))
                .andExpect(jsonPath("$.collageImages[2].shotType").value("PRODUCT"))
                .andExpect(jsonPath("$.collageImages[3].shotType").value("PRODUCT"));
    }

    @Test
    void 여러_제품을_태그하면_한_제품에_몰리지_않고_골고루_섞인다() throws Exception {
        Session session = newSession("mingyu");

        String styleA = newStyleNumber();
        Sku skuA = newSku(newProduct(), styleA);
        recordScan(session, skuA, 1);
        for (int position = 1; position <= 3; position++) {
            newSkuImage(styleA, position, ShotType.PRODUCT);
        }

        String styleB = newStyleNumber();
        Sku skuB = newSku(newProduct(), styleB);
        recordScan(session, skuB, 2);
        newSkuImage(styleB, 1, ShotType.PRODUCT);

        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collageImages.length()").value(4))
                // 라운드로빈: A 1장, B 1장, A 1장, A 1장 순으로 채워져 B의 유일한 이미지가 두 번째로 뽑힌다
                .andExpect(jsonPath("$.collageImages[1].imageUrl")
                        .value(skuImageRepository.findByStyleNumberIn(java.util.List.of(styleB)).get(0)
                                .getImageUrl()));
    }

    @Test
    void sessionCode는_태그_개수와_무관하게_세션ID_앞_5글자다() throws Exception {
        Session session = newSession("mingyu");
        String styleNumber = newStyleNumber();
        Sku sku = newSku(newProduct(), styleNumber);
        recordScan(session, sku, 1);
        recordScan(session, newSku(newProduct(), newStyleNumber()), 2);
        recordScan(session, newSku(newProduct(), newStyleNumber()), 3);

        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionCode").value(session.getSessionId().substring(0, 5)));
    }

    private Session newSession(String nickname) {
        Session session = sessionRepository.saveAndFlush(Session.builder()
                .sessionId(UUID.randomUUID().toString())
                .language("ko")
                .build());
        session.changeNickname(nickname);
        return sessionRepository.saveAndFlush(session);
    }

    private Product newProduct() {
        return productRepository.save(Product.builder()
                .name("테스트 상품 " + UUID.randomUUID())
                .category("bag")
                .build());
    }

    private Sku newSku(Product product, String styleNumber) {
        return skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet())
                .product(product)
                .color("Cognac")
                .size("ONE")
                .price(200_000)
                .stockQty(5)
                .styleNumber(styleNumber)
                .build());
    }

    private String newStyleNumber() {
        return "TSTYLE" + STYLE_NUMBER_SEQUENCE.incrementAndGet();
    }

    private void recordScan(Session session, Sku sku, int scanOrder) {
        tagScanLogRepository.save(TagScanLog.builder()
                .session(session)
                .sku(sku)
                .scanOrder(scanOrder)
                .scannedAt(LocalDateTime.now())
                .build());
    }

    private String newSkuImage(String styleNumber, int position, ShotType shotType) {
        String imageUrl = "https://cdn.example.com/" + styleNumber + "/" + position + ".webp";
        skuImageRepository.save(SkuImage.builder()
                .styleNumber(styleNumber)
                .position(position)
                .imageUrl(imageUrl)
                .shotType(shotType)
                .hasPerson(shotType == ShotType.MODEL)
                .build());
        return imageUrl;
    }
}