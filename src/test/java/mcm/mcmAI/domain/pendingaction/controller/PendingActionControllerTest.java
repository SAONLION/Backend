package mcm.mcmAI.domain.pendingaction.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import mcm.mcmAI.domain.pendingaction.entity.PendingAction;
import mcm.mcmAI.domain.pendingaction.entity.PendingActionOption;
import mcm.mcmAI.domain.pendingaction.repository.PendingActionRepository;
import mcm.mcmAI.domain.pendingaction.type.ActionNextStep;
import mcm.mcmAI.domain.pendingaction.type.BlockerType;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PendingActionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private PendingActionRepository pendingActionRepository;

    @Test
    void 대기중인_액션이_있으면_hasAction_true와_액션상세를_반환한다() throws Exception {
        Session session = sessionRepository.save(Session.builder()
                .sessionId(UUID.randomUUID().toString())
                .language("ko")
                .build());

        pendingActionRepository.save(PendingAction.builder()
                .session(session)
                .blockerType(BlockerType.CB5)
                .popupTitle("관심있어 하시던 백팩")
                .popupBody("비 오는 날에도 젖지 않는다는 것, 알고 계셨나요?")
                .options(List.of(
                        new PendingActionOption("ask_staff", "궁금해요, 직원에게 더 물어보기", ActionNextStep.STAFF_CALL_CREATED),
                        new PendingActionOption("other_product", "관심없어요 / 다른 제품 보기", ActionNextStep.SHOW_RECOMMENDATIONS)
                ))
                .build());

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(true))
                .andExpect(jsonPath("$.action.blockerType").value("CB5"))
                .andExpect(jsonPath("$.action.popupTitle").value("관심있어 하시던 백팩"))
                .andExpect(jsonPath("$.action.options[0].key").value("ask_staff"))
                .andExpect(jsonPath("$.action.options[1].label").value("관심없어요 / 다른 제품 보기"));
    }

    @Test
    void 대기중인_액션이_없으면_hasAction_false를_반환한다() throws Exception {
        Session session = sessionRepository.save(Session.builder()
                .sessionId(UUID.randomUUID().toString())
                .language("ko")
                .build());

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(false))
                .andExpect(jsonPath("$.action").doesNotExist());
    }

    @Test
    void 세션이_존재하지_않으면_404와_SESSION_NOT_FOUND_코드를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", "no-such-session"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }
}