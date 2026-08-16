package mcm.mcmAI.domain.staffcall.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import mcm.mcmAI.domain.product.entity.Product;
import mcm.mcmAI.domain.product.repository.ProductRepository;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.staffcall.entity.StaffCall;
import mcm.mcmAI.domain.staffcall.repository.StaffCallRepository;
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
class StaffCallControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StaffCallRepository staffCallRepository;

    @Test
    void 호출_요청_상태조회_시연용_상태변경_재조회까지_흐름이_모두_반영된다() throws Exception {
        Session session = newSession();
        Product product = newProduct();

        String createBody = """
                {"productId": %d, "reason": "가격 문의"}
                """.formatted(product.getProductId());

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
    void productId가_없으면_400과_MISSING_PRODUCT_ID_코드를_반환한다() throws Exception {
        Session session = newSession();

        mockMvc.perform(post("/api/v1/session/staff-calls")
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"가격 문의\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PRODUCT_ID"));
    }

    @Test
    void 존재하지_않는_productId면_404와_PRODUCT_NOT_FOUND_코드를_반환한다() throws Exception {
        Session session = newSession();

        mockMvc.perform(post("/api/v1/session/staff-calls")
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 999999999, \"reason\":\"가격 문의\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void 다른_세션의_callId로_조회하면_404와_CALL_NOT_FOUND_코드를_반환한다() throws Exception {
        Session ownerSession = newSession();
        Product product = newProduct();

        StaffCall staffCall = staffCallRepository.save(StaffCall.builder()
                .session(ownerSession)
                .product(product)
                .reason("가격 문의")
                .build());

        Session otherSession = newSession();

        mockMvc.perform(get("/api/v1/session/staff-calls/{callId}", staffCall.getCallId())
                        .param("sessionId", otherSession.getSessionId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CALL_NOT_FOUND"));
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
}