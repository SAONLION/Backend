package mcm.mcmAI.domain.contact.controller;

import mcm.mcmAI.support.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContactControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @Test
    void 이메일이_유효하면_연락처를_저장하고_201과_발송정보를_반환한다() throws Exception {
        Session session = newSession();

        String body = """
                {
                  "actionId": 9001,
                  "productId": 101,
                  "email": "guest@example.com",
                  "contentTopic": "Care & Styling Content"
                }
                """;

        mockMvc.perform(post("/api/v1/session/contacts")
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contactId").exists())
                .andExpect(jsonPath("$.contentSent").value(true))
                .andExpect(jsonPath("$.sentAt").exists());
    }

    @Test
    void 이메일_형식이_올바르지_않으면_400과_INVALID_EMAIL_코드를_반환한다() throws Exception {
        Session session = newSession();

        String body = """
                {
                  "email": "not-an-email"
                }
                """;

        mockMvc.perform(post("/api/v1/session/contacts")
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_EMAIL"));
    }

    @Test
    void 이메일이_없으면_400과_MISSING_CONTACT_INFO_코드를_반환한다() throws Exception {
        Session session = newSession();

        String body = """
                {
                  "contentTopic": "Care & Styling Content"
                }
                """;

        mockMvc.perform(post("/api/v1/session/contacts")
                        .param("sessionId", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_CONTACT_INFO"));
    }

    private Session newSession() {
        return sessionRepository.save(Session.builder()
                .sessionId(UUID.randomUUID().toString())
                .language("ko")
                .build());
    }
}