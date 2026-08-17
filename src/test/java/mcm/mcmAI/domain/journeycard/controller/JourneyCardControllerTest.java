package mcm.mcmAI.domain.journeycard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import mcm.mcmAI.domain.interactionlog.entity.InteractionLog;
import mcm.mcmAI.domain.interactionlog.repository.InteractionLogRepository;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.product.type.InterestType;
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

    @Autowired
    private InteractionLogRepository interactionLogRepository;

    @Test
    void 세션이_존재하지_않으면_404와_SESSION_NOT_FOUND_코드를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", "no-such-session"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void 태그_이력이_없으면_빈_콜라주를_반환하고_isComplete는_false다() throws Exception {
        Session session = newSession("mingyu");

        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand").value("MCM"))
                .andExpect(jsonPath("$.date").value(session.getCreatedAt().format(DATE_FORMATTER)))
                .andExpect(jsonPath("$.nickname").value("mingyu"))
                .andExpect(jsonPath("$.sessionCode").value(session.getSessionId().substring(0, 5)))
                .andExpect(jsonPath("$.collageImages").isArray())
                .andExpect(jsonPath("$.collageImages").isEmpty())
                .andExpect(jsonPath("$.isComplete").value(false));
    }

    @Test
    void 관심도_점수가_높은_제품_순으로_콜라주가_구성된다() throws Exception {
        Session session = newSession("mingyu");

        // 태그 순서는 A -> B -> C 지만, 관심도 점수는 C > B > A가 되도록 구성한다.
        String styleA = newStyleNumber();
        Sku skuA = newSku(newProduct(), styleA);
        recordScan(session, skuA, 1);
        String imageA = newSkuImage(styleA, 1, ShotType.PRODUCT);

        String styleB = newStyleNumber();
        Sku skuB = newSku(newProduct(), styleB);
        recordScan(session, skuB, 2);
        String imageB = newSkuImage(styleB, 1, ShotType.PRODUCT);
        recordInteraction(session, skuB, 10);

        String styleC = newStyleNumber();
        Sku skuC = newSku(newProduct(), styleC);
        recordScan(session, skuC, 3);
        String imageC = newSkuImage(styleC, 1, ShotType.PRODUCT);
        recordInteraction(session, skuC, 100);

        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collageImages.length()").value(3))
                .andExpect(jsonPath("$.collageImages[0].imageUrl").value(imageC))
                .andExpect(jsonPath("$.collageImages[1].imageUrl").value(imageB))
                .andExpect(jsonPath("$.collageImages[2].imageUrl").value(imageA))
                .andExpect(jsonPath("$.isComplete").value(false));
    }

    @Test
    void interaction_기록이_있는_제품이_먼저_태그했지만_기록이_없는_제품보다_높은_순위를_가진다() throws Exception {
        Session session = newSession("mingyu");

        String styleTaggedFirst = newStyleNumber();
        Sku skuTaggedFirst = newSku(newProduct(), styleTaggedFirst);
        recordScan(session, skuTaggedFirst, 1);
        String imageTaggedFirst = newSkuImage(styleTaggedFirst, 1, ShotType.PRODUCT);

        String styleTaggedSecond = newStyleNumber();
        Sku skuTaggedSecond = newSku(newProduct(), styleTaggedSecond);
        recordScan(session, skuTaggedSecond, 2);
        String imageTaggedSecond = newSkuImage(styleTaggedSecond, 1, ShotType.PRODUCT);
        recordInteraction(session, skuTaggedSecond, 5);
        recordInteraction(session, skuTaggedSecond, 5);

        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collageImages.length()").value(2))
                .andExpect(jsonPath("$.collageImages[0].imageUrl").value(imageTaggedSecond))
                .andExpect(jsonPath("$.collageImages[1].imageUrl").value(imageTaggedFirst));
    }

    @Test
    void 태그한_제품이_4개_미만이면_있는_만큼만_채우고_isComplete는_false다() throws Exception {
        Session session = newSession("mingyu");

        String styleA = newStyleNumber();
        Sku skuA = newSku(newProduct(), styleA);
        recordScan(session, skuA, 1);
        newSkuImage(styleA, 1, ShotType.PRODUCT);

        String styleB = newStyleNumber();
        Sku skuB = newSku(newProduct(), styleB);
        recordScan(session, skuB, 2);
        newSkuImage(styleB, 1, ShotType.PRODUCT);

        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collageImages.length()").value(2))
                .andExpect(jsonPath("$.isComplete").value(false));
    }

    @Test
    void 관심도_상위_4개_제품의_이미지로_4장이_채워지면_isComplete는_true다() throws Exception {
        Session session = newSession("mingyu");

        // 1위: 정면샷(position 가장 앞선 PRODUCT)
        String style1st = newStyleNumber();
        Sku sku1st = newSku(newProduct(), style1st);
        recordScan(session, sku1st, 4);
        String frontShot = newSkuImage(style1st, 1, ShotType.PRODUCT);
        newSkuImage(style1st, 2, ShotType.PRODUCT);
        recordInteraction(session, sku1st, 100);

        // 2위: 다른 각도샷(position 두 번째로 앞선 PRODUCT)
        String style2nd = newStyleNumber();
        Sku sku2nd = newSku(newProduct(), style2nd);
        recordScan(session, sku2nd, 3);
        newSkuImage(style2nd, 1, ShotType.PRODUCT);
        String alternateShot = newSkuImage(style2nd, 2, ShotType.PRODUCT);
        recordInteraction(session, sku2nd, 50);

        // 3위: 모델샷 우선
        String style3rd = newStyleNumber();
        Sku sku3rd = newSku(newProduct(), style3rd);
        recordScan(session, sku3rd, 2);
        String modelShot = newSkuImage(style3rd, 1, ShotType.MODEL);
        newSkuImage(style3rd, 2, ShotType.PRODUCT);
        recordInteraction(session, sku3rd, 10);

        // 4위: 컨셉샷 슬롯(현재는 4위 제품 이미지로 대체)
        String style4th = newStyleNumber();
        Sku sku4th = newSku(newProduct(), style4th);
        recordScan(session, sku4th, 1);
        String conceptSlotShot = newSkuImage(style4th, 1, ShotType.PRODUCT);

        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collageImages.length()").value(4))
                .andExpect(jsonPath("$.collageImages[0].imageUrl").value(frontShot))
                .andExpect(jsonPath("$.collageImages[0].shotType").value("PRODUCT"))
                .andExpect(jsonPath("$.collageImages[1].imageUrl").value(alternateShot))
                .andExpect(jsonPath("$.collageImages[2].imageUrl").value(modelShot))
                .andExpect(jsonPath("$.collageImages[2].shotType").value("MODEL"))
                .andExpect(jsonPath("$.collageImages[3].imageUrl").value(conceptSlotShot))
                .andExpect(jsonPath("$.isComplete").value(true));
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

    private void recordInteraction(Session session, Sku sku, int durationSeconds) {
        interactionLogRepository.save(InteractionLog.builder()
                .session(session)
                .sku(sku)
                .interestType(InterestType.PRODUCT_UNDERSTANDING)
                .durationSeconds(durationSeconds)
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
