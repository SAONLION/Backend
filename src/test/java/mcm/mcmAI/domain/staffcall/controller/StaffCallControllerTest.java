package mcm.mcmAI.domain.staffcall.controller;

import mcm.mcmAI.support.AbstractIntegrationTest;

import static mcm.mcmAI.global.security.StaffBoardTokenInterceptor.TOKEN_HEADER;
import static mcm.mcmAI.support.AbstractIntegrationTest.STAFF_BOARD_TEST_TOKEN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.sku.entity.Sku;
import mcm.mcmAI.domain.sku.repository.SkuRepository;
import mcm.mcmAI.domain.staffcall.entity.StaffCall;
import mcm.mcmAI.domain.staffcall.repository.StaffCallRepository;
import mcm.mcmAI.domain.staffcall.type.StaffCallStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.internal-test-endpoints.enabled=true")
class StaffCallControllerTest extends AbstractIntegrationTest {

    private static final AtomicLong SKU_ID_SEQUENCE = new AtomicLong(980_000_000L);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SkuRepository skuRepository;

    @Autowired
    private StaffCallRepository staffCallRepository;

    @Test
    void 호출_요청_상태조회_시연용_상태변경_재조회까지_흐름이_모두_반영된다() throws Exception {
        Session session = newSession();
        Sku sku = newSku(newProduct());

        String createBody = """
                {"sku": %d, "reason": "가격 문의"}
                """.formatted(sku.getSku());

        mockMvc.perform(post("/api/v1/session/staff-calls")
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("requested"))
                .andExpect(jsonPath("$.requestedAt").exists());

        List<StaffCall> calls = staffCallRepository.findBySession_SessionId(session.getSessionId());
        Long callId = calls.get(0).getCallId();

        mockMvc.perform(get("/api/v1/session/staff-calls/{callId}", callId)
                        .param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("requested"))
                .andExpect(jsonPath("$.displayMessage").value("직원 호출을 요청했어요"));

        mockMvc.perform(patch("/internal/test/staff-calls/{callId}/status", callId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"in_progress\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("in_progress"));

        mockMvc.perform(get("/api/v1/session/staff-calls/{callId}", callId)
                        .param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("in_progress"))
                .andExpect(jsonPath("$.displayMessage").value("직원이 제품을 준비해서 가는 중이에요"));
    }

    @Test
    void sku가_없어도_reason만_있으면_호출이_생성된다() throws Exception {
        Session session = newSession();

        mockMvc.perform(post("/api/v1/session/staff-calls")
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"기타 문의\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("requested"))
                .andExpect(jsonPath("$.requestedAt").exists());

        List<StaffCall> calls = staffCallRepository.findBySession_SessionId(session.getSessionId());
        StaffCall call = calls.get(0);
        org.assertj.core.api.Assertions.assertThat(call.getSku()).isNull();

        mockMvc.perform(get("/api/v1/staff/staff-calls")
                        .header(TOKEN_HEADER, STAFF_BOARD_TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waiting[0].callId").value(call.getCallId()))
                .andExpect(jsonPath("$.waiting[0].productName").doesNotExist())
                .andExpect(jsonPath("$.waiting[0].color").doesNotExist());
    }

    @Test
    void reason이_없으면_400을_반환한다() throws Exception {
        Session session = newSession();

        mockMvc.perform(post("/api/v1/session/staff-calls")
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 존재하지_않는_sku면_404와_SKU_NOT_FOUND_코드를_반환한다() throws Exception {
        Session session = newSession();

        mockMvc.perform(post("/api/v1/session/staff-calls")
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\": 999999999, \"reason\":\"가격 문의\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SKU_NOT_FOUND"));
    }

    @Test
    void 다른_세션의_callId로_조회하면_404와_CALL_NOT_FOUND_코드를_반환한다() throws Exception {
        Session ownerSession = newSession();
        Sku sku = newSku(newProduct());

        StaffCall staffCall = staffCallRepository.save(StaffCall.builder()
                .session(ownerSession)
                .sku(sku)
                .reason("가격 문의")
                .build());

        Session otherSession = newSession();

        mockMvc.perform(get("/api/v1/session/staff-calls/{callId}", staffCall.getCallId())
                        .param("sessionId", otherSession.getSessionId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CALL_NOT_FOUND"));
    }

    @Test
    void 보드_조회는_대기_호출을_요청순으로_완료_호출을_최근순으로_반환한다() throws Exception {
        Session session = newSession();
        Sku sku = newSku(newProduct());

        StaffCall waitingCall = staffCallRepository.save(StaffCall.builder()
                .session(session).sku(sku).reason("대기 중 호출").build());

        StaffCall completedCall = staffCallRepository.save(StaffCall.builder()
                .session(session).sku(sku).reason("완료된 호출").build());
        completedCall.changeStatus(StaffCallStatus.COMPLETED);

        mockMvc.perform(get("/api/v1/staff/staff-calls")
                        .header(TOKEN_HEADER, STAFF_BOARD_TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waiting[0].callId").value(waitingCall.getCallId()))
                .andExpect(jsonPath("$.waiting[0].productName").exists())
                .andExpect(jsonPath("$.completed[0].callId").value(completedCall.getCallId()))
                .andExpect(jsonPath("$.completed[0].status").value("completed"));
    }

    @Test
    void 완료_처리_API를_호출하면_상태가_completed로_바뀐다() throws Exception {
        Session session = newSession();
        Sku sku = newSku(newProduct());

        StaffCall staffCall = staffCallRepository.save(StaffCall.builder()
                .session(session).sku(sku).reason("가격 문의").build());

        mockMvc.perform(patch("/api/v1/staff/staff-calls/{callId}/complete", staffCall.getCallId())
                        .header(TOKEN_HEADER, STAFF_BOARD_TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));
    }

    @Test
    void 존재하지_않는_callId를_완료처리하면_404와_CALL_NOT_FOUND_코드를_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/staff/staff-calls/{callId}/complete", 999999999L)
                        .header(TOKEN_HEADER, STAFF_BOARD_TEST_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CALL_NOT_FOUND"));
    }

    @Test
    void 태블릿_토큰이_없거나_틀리면_401과_STAFF_TOKEN_INVALID_코드를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/staff/staff-calls"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("STAFF_TOKEN_INVALID"));

        mockMvc.perform(get("/api/v1/staff/staff-calls")
                        .header(TOKEN_HEADER, "wrong-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("STAFF_TOKEN_INVALID"));
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
