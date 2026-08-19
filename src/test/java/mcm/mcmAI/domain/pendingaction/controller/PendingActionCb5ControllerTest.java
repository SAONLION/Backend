package mcm.mcmAI.domain.pendingaction.controller;

import mcm.mcmAI.support.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import mcm.mcmAI.domain.interactionlog.entity.InteractionLog;
import mcm.mcmAI.domain.interactionlog.repository.InteractionLogRepository;
import mcm.mcmAI.domain.pendingaction.repository.PendingActionRepository;
import mcm.mcmAI.domain.pendingaction.type.BlockerType;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.product.type.InterestType;
import mcm.mcmAI.domain.purchaseinquiry.entity.PurchaseInquiry;
import mcm.mcmAI.domain.purchaseinquiry.repository.PurchaseInquiryRepository;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import mcm.mcmAI.domain.staffcall.entity.StaffCall;
import mcm.mcmAI.domain.staffcall.repository.StaffCallRepository;
import mcm.mcmAI.domain.tagscanlog.entity.TagScanLog;
import mcm.mcmAI.domain.tagscanlog.repository.TagScanLogRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PendingActionCb5ControllerTest extends AbstractIntegrationTest {

    private static final AtomicLong SKU_ID_SEQUENCE = new AtomicLong(960_000_000L);

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
    private InteractionLogRepository interactionLogRepository;

    @Autowired
    private PurchaseInquiryRepository purchaseInquiryRepository;

    @Autowired
    private StaffCallRepository staffCallRepository;

    @Autowired
    private PendingActionRepository pendingActionRepository;

    // ===== T-CB5-1: 가격 공개 후 동일 카테고리 하위 가격대로 태그 전환 =====

    @Test
    void 동일_가격대로_전환하면_CB5_1이_생성되지_않는다() throws Exception {
        Session session = newSession();
        Product category = newProduct("bag");
        Sku disclosedSku = newSku(category, 1_000_000);
        priceDisclosure(session, disclosedSku, LocalDateTime.now().minusMinutes(3));

        // 20% 임계값에 못 미치는, 10%만 저렴한 스위치
        Sku switchedSku = newSku(category, 900_000);
        newTagScan(session, switchedSku, LocalDateTime.now().minusMinutes(1));

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(false));
    }

    @Test
    void 가격공개_후_동일카테고리_하위가격대로_전환하면_CB5_1이_생성된다() throws Exception {
        Session session = newSession();
        Product category = newProduct("bag");
        Sku disclosedSku = newSku(category, 1_000_000);
        priceDisclosure(session, disclosedSku, LocalDateTime.now().minusMinutes(3));

        Sku cheaperSku = newSku(category, 700_000);
        TagScanLog scan = newTagScan(session, cheaperSku, LocalDateTime.now().minusMinutes(1));

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(true))
                .andExpect(jsonPath("$.action.blockerType").value("CB5"))
                .andExpect(jsonPath("$.action.options[0].key").value("recommend_alt"));

        boolean exists = pendingActionRepository
                .existsByTriggerTagScanLog_ScanIdAndBlockerType(scan.getScanId(), BlockerType.CB5);
        Assertions.assertTrue(exists);
    }

    @Test
    void 구매문의가_발생했으면_CB5_1이_생성되지_않는다() throws Exception {
        Session session = newSession();
        Product category = newProduct("bag");
        Sku disclosedSku = newSku(category, 1_000_000);
        InteractionLog disclosure = priceDisclosure(session, disclosedSku, LocalDateTime.now().minusMinutes(5));

        Sku cheaperSku = newSku(category, 700_000);
        TagScanLog scan = newTagScan(session, cheaperSku, LocalDateTime.now().minusMinutes(1));

        purchaseInquiryRepository.save(PurchaseInquiry.builder()
                .session(session)
                .sku(disclosedSku)
                .build());

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(false));

        boolean exists = pendingActionRepository
                .existsByTriggerTagScanLog_ScanIdAndBlockerType(scan.getScanId(), BlockerType.CB5);
        Assertions.assertFalse(exists);
        Assertions.assertNotNull(disclosure);
    }

    @Test
    void 이미_생성된_CB5_1은_다시_조회해도_중복_생성되지_않는다() throws Exception {
        Session session = newSession();
        Product category = newProduct("bag");
        Sku disclosedSku = newSku(category, 1_000_000);
        priceDisclosure(session, disclosedSku, LocalDateTime.now().minusMinutes(3));

        Sku cheaperSku = newSku(category, 700_000);
        newTagScan(session, cheaperSku, LocalDateTime.now().minusMinutes(1));

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(true));

        long countAfterFirstPoll = pendingActionRepository.count();

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(true));

        long countAfterSecondPoll = pendingActionRepository.count();

        Assertions.assertEquals(countAfterFirstPoll, countAfterSecondPoll);
    }

    // ===== T-CB5-2: 가격 공개 후 10분간 신규 이벤트 없음 =====

    @Test
    void 가격공개_10분_이내는_CB5_2가_생성되지_않는다() throws Exception {
        Session session = newSession();
        Product product = newProduct("bag");
        Sku sku = newSku(product, 1_000_000);
        priceDisclosure(session, sku, LocalDateTime.now().minusMinutes(5));

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(false));
    }

    @Test
    void 가격공개_후_10분간_신규이벤트가_없으면_CB5_2가_생성된다() throws Exception {
        Session session = newSession();
        Product product = newProduct("bag");
        Sku sku = newSku(product, 1_000_000);
        InteractionLog disclosure = priceDisclosure(session, sku, LocalDateTime.now().minusMinutes(11));

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(true))
                .andExpect(jsonPath("$.action.blockerType").value("CB5"))
                .andExpect(jsonPath("$.action.options[0].key").value("ask_staff"));

        boolean exists = pendingActionRepository
                .existsByTriggerInteractionLog_InteractionIdAndBlockerType(
                        disclosure.getInteractionId(), BlockerType.CB5);
        Assertions.assertTrue(exists);
    }

    @Test
    void 프로모션_관련_직원호출이_있으면_CB5_2가_생성되지_않는다() throws Exception {
        Session session = newSession();
        Product product = newProduct("bag");
        Sku sku = newSku(product, 1_000_000);
        InteractionLog disclosure = priceDisclosure(session, sku, LocalDateTime.now().minusMinutes(11));

        staffCallRepository.save(StaffCall.builder()
                .session(session)
                .reason("할인/프로모션 안내 문의")
                .build());

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(false));

        boolean exists = pendingActionRepository
                .existsByTriggerInteractionLog_InteractionIdAndBlockerType(
                        disclosure.getInteractionId(), BlockerType.CB5);
        Assertions.assertFalse(exists);
    }

    @Test
    void 이미_생성된_CB5_2는_다시_조회해도_중복_생성되지_않는다() throws Exception {
        Session session = newSession();
        Product product = newProduct("bag");
        Sku sku = newSku(product, 1_000_000);
        priceDisclosure(session, sku, LocalDateTime.now().minusMinutes(11));

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(true));

        long countAfterFirstPoll = pendingActionRepository.count();

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(true));

        long countAfterSecondPoll = pendingActionRepository.count();

        Assertions.assertEquals(countAfterFirstPoll, countAfterSecondPoll);
    }

    private Session newSession() {
        return sessionRepository.save(Session.builder()
                .sessionId(UUID.randomUUID().toString())
                .language("ko")
                .build());
    }

    private Product newProduct(String category) {
        return productRepository.save(Product.builder()
                .name("테스트 상품")
                .category(category)
                .build());
    }

    private Sku newSku(Product product, Integer price) {
        return skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet())
                .product(product)
                .color("Black")
                .size("ONE")
                .price(price)
                .stockQty(5)
                .build());
    }

    private InteractionLog priceDisclosure(Session session, Sku sku, LocalDateTime createdAt) {
        InteractionLog interactionLog = interactionLogRepository.save(InteractionLog.builder()
                .session(session)
                .sku(sku)
                .interestType(InterestType.PURCHASE_CONDITION)
                .subOption("PRICE")
                .build());
        interactionLogRepository.forceCreatedAtForTest(interactionLog.getInteractionId(), createdAt);
        return interactionLog;
    }

    private TagScanLog newTagScan(Session session, Sku sku, LocalDateTime scannedAt) {
        return tagScanLogRepository.save(TagScanLog.builder()
                .session(session)
                .sku(sku)
                .scanOrder((int) tagScanLogRepository.countBySession_SessionId(session.getSessionId()) + 1)
                .scannedAt(scannedAt)
                .build());
    }
}
