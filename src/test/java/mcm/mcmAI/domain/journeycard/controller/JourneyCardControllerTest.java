package mcm.mcmAI.domain.journeycard.controller;

import mcm.mcmAI.support.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import mcm.mcmAI.domain.contact.entity.Contact;
import mcm.mcmAI.domain.contact.repository.ContactRepository;
import mcm.mcmAI.domain.interactionlog.entity.InteractionLog;
import mcm.mcmAI.domain.interactionlog.repository.InteractionLogRepository;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.product.type.InterestType;
import mcm.mcmAI.domain.purchaseinquiry.entity.PurchaseInquiry;
import mcm.mcmAI.domain.purchaseinquiry.repository.PurchaseInquiryRepository;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import mcm.mcmAI.domain.skuimage.entity.SkuImage;
import mcm.mcmAI.domain.skuimage.repository.SkuImageRepository;
import mcm.mcmAI.domain.skuimage.type.ShotType;
import mcm.mcmAI.domain.tagscanlog.entity.TagScanLog;
import mcm.mcmAI.domain.tagscanlog.repository.TagScanLogRepository;
import mcm.mcmAI.domain.tryonrequest.entity.TryonRequest;
import mcm.mcmAI.domain.tryonrequest.repository.TryonRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JourneyCardControllerTest extends AbstractIntegrationTest {

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

    @Autowired
    private PurchaseInquiryRepository purchaseInquiryRepository;

    @Autowired
    private TryonRequestRepository tryonRequestRepository;

    @Autowired
    private ContactRepository contactRepository;

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
                .andExpect(jsonPath("$.isComplete").value(false))
                .andExpect(jsonPath("$.favoriteColor").doesNotExist());
    }

    @Test
    void 가장_많이_태그_스캔된_색상이_favoriteColor로_반환된다() throws Exception {
        Session session = newSession("mingyu");

        // Cognac: 2회 스캔 (styleA + styleB의 재방문), Black: 1회 스캔 -> Cognac이 우세해야 한다.
        String styleA = newStyleNumber();
        Sku skuA = newSku(newProduct(), styleA, "Cognac");
        recordScan(session, skuA, 1);

        String styleBlack = newStyleNumber();
        Sku skuBlack = newSku(newProduct(), styleBlack, "Black");
        recordScan(session, skuBlack, 2);

        String styleB = newStyleNumber();
        Sku skuB = newSku(newProduct(), styleB, "Cognac");
        recordScan(session, skuB, 3);

        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteColor.code").value("COGNAC"))
                .andExpect(jsonPath("$.favoriteColor.label").value("Cognac"))
                .andExpect(jsonPath("$.favoriteColor.tagCount").value(2));
    }

    @Test
    void 색상별_태그_횟수가_동률이면_가장_최근_태그한_색상이_favoriteColor다() throws Exception {
        Session session = newSession("mingyu");

        String styleA = newStyleNumber();
        Sku skuA = newSku(newProduct(), styleA, "Cognac");
        recordScan(session, skuA, 1);

        String styleBlack = newStyleNumber();
        Sku skuBlack = newSku(newProduct(), styleBlack, "Black");
        recordScan(session, skuBlack, 2);

        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteColor.code").value("BLACK"))
                .andExpect(jsonPath("$.favoriteColor.label").value("Black"))
                .andExpect(jsonPath("$.favoriteColor.tagCount").value(1));
    }

    @Test
    void 관심도_점수가_높은_제품_순으로_콜라주가_구성된다() throws Exception {
        Session session = newSession("mingyu");

        // 태그 순서는 A -> B -> C 지만, 관심도 점수는 C(태그5+문의35+체험30=70) > B(태그5+문의35=40) > A(태그만=5)가 되도록 구성한다.
        String styleA = newStyleNumber();
        Sku skuA = newSku(newProduct(), styleA);
        recordScan(session, skuA, 1);
        String imageA = newSkuImage(styleA, 1, ShotType.PRODUCT);

        String styleB = newStyleNumber();
        Sku skuB = newSku(newProduct(), styleB);
        recordScan(session, skuB, 2);
        String imageB = newSkuImage(styleB, 1, ShotType.PRODUCT);
        newPurchaseInquiry(session, skuB);

        String styleC = newStyleNumber();
        Sku skuC = newSku(newProduct(), styleC);
        recordScan(session, skuC, 3);
        String imageC = newSkuImage(styleC, 1, ShotType.PRODUCT);
        newPurchaseInquiry(session, skuC);
        newTryonRequest(session, skuC);

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
        recordHubClick(session, skuTaggedSecond);
        recordHubClick(session, skuTaggedSecond);

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

        // 태그 순서는 4위->3위->2위->1위 역순이지만, 관심도 점수는 1위>2위>3위>4위가 되도록 구성한다.
        // 1위: 태그(5)+구매문의(35)=40, 정면샷(position 가장 앞선 PRODUCT)
        String style1st = newStyleNumber();
        Sku sku1st = newSku(newProduct(), style1st);
        recordScan(session, sku1st, 4);
        String frontShot = newSkuImage(style1st, 1, ShotType.PRODUCT);
        newSkuImage(style1st, 2, ShotType.PRODUCT);
        newPurchaseInquiry(session, sku1st);

        // 2위: 태그(5)+트라이온요청(30)=35, 다른 각도샷(position 두 번째로 앞선 PRODUCT)
        String style2nd = newStyleNumber();
        Sku sku2nd = newSku(newProduct(), style2nd);
        recordScan(session, sku2nd, 3);
        newSkuImage(style2nd, 1, ShotType.PRODUCT);
        String alternateShot = newSkuImage(style2nd, 2, ShotType.PRODUCT);
        newTryonRequest(session, sku2nd);

        // 3위: 태그(5)+hub 클릭 1회(2)=7, 모델샷 우선
        String style3rd = newStyleNumber();
        Sku sku3rd = newSku(newProduct(), style3rd);
        recordScan(session, sku3rd, 2);
        String modelShot = newSkuImage(style3rd, 1, ShotType.MODEL);
        newSkuImage(style3rd, 2, ShotType.PRODUCT);
        recordHubClick(session, sku3rd);

        // 4위: 태그만(5), 컨셉샷 슬롯(현재는 4위 제품 이미지로 대체)
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
    void 동일_SKU_재방문_시_태그스캔_점수에_가산된다() throws Exception {
        Session session = newSession("mingyu");

        // 재방문 SKU: 태그(5) + 재방문(10) = 15점
        String styleRevisit = newStyleNumber();
        Sku skuRevisit = newSku(newProduct(), styleRevisit);
        recordScan(session, skuRevisit, 1);
        recordScan(session, skuRevisit, 3);
        String imageRevisit = newSkuImage(styleRevisit, 1, ShotType.PRODUCT);

        // 비교군: 태그(5) + hub 클릭 3회(3x2=6) = 11점. 재방문이 +10 가산(=15)이 아니라
        // 기존 +5를 대체(=10)하는 것이었다면 이 비교군(11점)에 밀려야 한다.
        String styleOnce = newStyleNumber();
        Sku skuOnce = newSku(newProduct(), styleOnce);
        recordScan(session, skuOnce, 2);
        String imageOnce = newSkuImage(styleOnce, 1, ShotType.PRODUCT);
        for (int i = 0; i < 3; i++) {
            recordHubClick(session, skuOnce);
        }

        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collageImages[0].imageUrl").value(imageRevisit))
                .andExpect(jsonPath("$.collageImages[1].imageUrl").value(imageOnce));
    }

    @Test
    void PRICE_조회는_존재_보너스와_subOption_카운트에_동시에_가산된다() throws Exception {
        Session session = newSession("mingyu");

        // PRICE 조회 SKU: 태그(5) + PRICE 존재(15) + subOption 카운트(1x3=3) = 23점 (중복 가산 시)
        String stylePrice = newStyleNumber();
        Sku skuPrice = newSku(newProduct(), stylePrice);
        recordScan(session, skuPrice, 1);
        String imagePrice = newSkuImage(stylePrice, 1, ShotType.PRODUCT);
        recordPriceInquiry(session, skuPrice);

        // 비교군: 태그(5) + (PRICE 아닌) subOption 클릭 6회(6x3=18) = 23점.
        // PRICE 존재 보너스가 subOption 카운트와 별개로 중복 가산되지 않는다면(=20점)
        // PRICE 조회 SKU는 이 비교군(23점)에 밀려야 한다.
        String styleCompare = newStyleNumber();
        Sku skuCompare = newSku(newProduct(), styleCompare);
        recordScan(session, skuCompare, 2);
        String imageCompare = newSkuImage(styleCompare, 1, ShotType.PRODUCT);
        for (int i = 0; i < 6; i++) {
            recordSubhubClick(session, skuCompare, InterestType.PRODUCT_UNDERSTANDING, "MATERIAL");
        }

        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collageImages[0].imageUrl").value(imagePrice))
                .andExpect(jsonPath("$.collageImages[1].imageUrl").value(imageCompare));
    }

    @Test
    void 같은_상품의_여러_SKU_점수는_상품_단위로_합산되고_대표_이미지는_먼저_태그한_SKU_것이다() throws Exception {
        Session session = newSession("mingyu");

        // productP: skuP1(태그+구매문의=5+35=40) + skuP2(태그만=5) = 45점
        Product productP = newProduct();
        String styleP1 = newStyleNumber();
        Sku skuP1 = newSku(productP, styleP1);
        recordScan(session, skuP1, 1);
        String imageP1 = newSkuImage(styleP1, 1, ShotType.PRODUCT);
        newPurchaseInquiry(session, skuP1);

        String styleP2 = newStyleNumber();
        Sku skuP2 = newSku(productP, styleP2);
        recordScan(session, skuP2, 2);
        newSkuImage(styleP2, 1, ShotType.PRODUCT);

        // productQ: 태그+트라이온요청 = 5+30 = 35점
        String styleQ = newStyleNumber();
        Sku skuQ = newSku(newProduct(), styleQ);
        recordScan(session, skuQ, 3);
        String imageQ = newSkuImage(styleQ, 1, ShotType.PRODUCT);
        newTryonRequest(session, skuQ);

        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collageImages.length()").value(2))
                .andExpect(jsonPath("$.collageImages[0].imageUrl").value(imageP1))
                .andExpect(jsonPath("$.collageImages[1].imageUrl").value(imageQ));
    }

    @Test
    void Contact_발송_보너스는_SKU가_아니라_상품당_1회만_가산된다() throws Exception {
        Session session = newSession("mingyu");

        // productP: skuP1(태그=5) + skuP2(태그=5) = 10점, Contact 보너스가 상품당 1회(+20)면 총 30점
        Product productP = newProduct();
        String styleP1 = newStyleNumber();
        Sku skuP1 = newSku(productP, styleP1);
        recordScan(session, skuP1, 1);
        String imageP1 = newSkuImage(styleP1, 1, ShotType.PRODUCT);

        String styleP2 = newStyleNumber();
        Sku skuP2 = newSku(productP, styleP2);
        recordScan(session, skuP2, 2);
        newSkuImage(styleP2, 1, ShotType.PRODUCT);

        newContact(session, productP.getProductId(), true);

        // productQ: 태그+트라이온요청 = 5+30 = 35점.
        // Contact 보너스가 SKU마다 중복 가산된다면(잘못) productP=10+40=50으로 productQ(35)를 앞질러야 하지만,
        // 상품당 1회만 가산되는 게 맞다면 productP=30 < productQ=35 라 productQ가 1위여야 한다.
        String styleQ = newStyleNumber();
        Sku skuQ = newSku(newProduct(), styleQ);
        recordScan(session, skuQ, 3);
        String imageQ = newSkuImage(styleQ, 1, ShotType.PRODUCT);
        newTryonRequest(session, skuQ);

        mockMvc.perform(get("/api/v1/session/journey-card").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collageImages[0].imageUrl").value(imageQ))
                .andExpect(jsonPath("$.collageImages[1].imageUrl").value(imageP1));
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
        return newSku(product, styleNumber, "Cognac");
    }

    private Sku newSku(Product product, String styleNumber, String color) {
        return skuRepository.save(Sku.builder()
                .sku(SKU_ID_SEQUENCE.incrementAndGet())
                .product(product)
                .color(color)
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

    // subOption == null: 1차 허브만 클릭 (hub 클릭, +2)
    private void recordHubClick(Session session, Sku sku) {
        interactionLogRepository.save(InteractionLog.builder()
                .session(session)
                .sku(sku)
                .interestType(InterestType.PRODUCT_UNDERSTANDING)
                .build());
    }

    // subOption != null: 2차 세부 옵션까지 클릭 (subhub 클릭, +3)
    private void recordSubhubClick(Session session, Sku sku, InterestType interestType, String subOption) {
        interactionLogRepository.save(InteractionLog.builder()
                .session(session)
                .sku(sku)
                .interestType(interestType)
                .subOption(subOption)
                .build());
    }

    // interestType=PURCHASE_CONDITION, subOption="PRICE" 존재 -> +15 (subhub 클릭 +3과 별개로 가산)
    private void recordPriceInquiry(Session session, Sku sku) {
        recordSubhubClick(session, sku, InterestType.PURCHASE_CONDITION, "PRICE");
    }

    private void newPurchaseInquiry(Session session, Sku sku) {
        purchaseInquiryRepository.save(PurchaseInquiry.builder()
                .session(session)
                .sku(sku)
                .build());
    }

    private void newTryonRequest(Session session, Sku sku) {
        tryonRequestRepository.save(TryonRequest.builder()
                .session(session)
                .sku(sku)
                .size("M")
                .color("Black")
                .build());
    }

    private void newContact(Session session, Long productId, boolean contentSent) {
        contactRepository.save(Contact.builder()
                .session(session)
                .productId(productId)
                .email("test@example.com")
                .contentSent(contentSent)
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
