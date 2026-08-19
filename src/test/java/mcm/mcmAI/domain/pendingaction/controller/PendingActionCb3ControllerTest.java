package mcm.mcmAI.domain.pendingaction.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;
import mcm.mcmAI.domain.pendingaction.repository.PendingActionRepository;
import mcm.mcmAI.domain.pendingaction.type.BlockerType;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import mcm.mcmAI.domain.staffcall.entity.StaffCall;
import mcm.mcmAI.domain.staffcall.repository.StaffCallRepository;
import mcm.mcmAI.domain.staffcall.type.StaffCallStatus;
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
class PendingActionCb3ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private StaffCallRepository staffCallRepository;

    @Autowired
    private PendingActionRepository pendingActionRepository;

    @Test
    void 직원호출_5분_이내_미응대는_CB3를_생성하지_않는다() throws Exception {
        Session session = newSession();
        newStaffCall(session, LocalDateTime.now().minusMinutes(3));

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(false));
    }

    @Test
    void 직원호출_5분_초과_미응대면_CB3가_생성된다() throws Exception {
        Session session = newSession();
        StaffCall staffCall = newStaffCall(session, LocalDateTime.now().minusMinutes(6));

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(true))
                .andExpect(jsonPath("$.action.blockerType").value("CB3"))
                .andExpect(jsonPath("$.action.popupTitle").value("직원에게 직접 안내를\n받아보시겠어요?"))
                .andExpect(jsonPath("$.action.popupBody").value("담당 직원을 우선적으로 호출해드릴까요?"))
                .andExpect(jsonPath("$.action.options[0].key").value("escalate_call"))
                .andExpect(jsonPath("$.action.options[0].label").value("우선 호출 요청"));

        boolean exists = pendingActionRepository
                .existsByStaffCall_CallIdAndBlockerType(staffCall.getCallId(), BlockerType.CB3);
        Assertions.assertTrue(exists);
    }

    @Test
    void 오분_초과했지만_이미_응대가_시작됐으면_CB3가_생성되지_않는다() throws Exception {
        Session session = newSession();
        StaffCall staffCall = newStaffCall(session, LocalDateTime.now().minusMinutes(10));
        staffCall.changeStatus(StaffCallStatus.ACKNOWLEDGED);
        staffCallRepository.save(staffCall);

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(false));

        boolean exists = pendingActionRepository
                .existsByStaffCall_CallIdAndBlockerType(staffCall.getCallId(), BlockerType.CB3);
        Assertions.assertFalse(exists);
    }

    @Test
    void 이미_CB3가_생성된_경우_다시_조회해도_중복_생성되지_않는다() throws Exception {
        Session session = newSession();
        StaffCall staffCall = newStaffCall(session, LocalDateTime.now().minusMinutes(6));

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(true));

        long countAfterFirstPoll = pendingActionRepository.count();

        mockMvc.perform(get("/api/v1/session/pending-action").param("sessionId", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAction").value(true))
                .andExpect(jsonPath("$.action.blockerType").value("CB3"));

        long countAfterSecondPoll = pendingActionRepository.count();

        Assertions.assertEquals(countAfterFirstPoll, countAfterSecondPoll);
        Assertions.assertEquals(1, staffCallRepository.findBySession_SessionIdAndStatus(
                session.getSessionId(), StaffCallStatus.REQUESTED).size());
        Assertions.assertNotNull(staffCall);
    }

    private Session newSession() {
        return sessionRepository.save(Session.builder()
                .sessionId(UUID.randomUUID().toString())
                .language("ko")
                .build());
    }

    private StaffCall newStaffCall(Session session, LocalDateTime requestedAt) {
        StaffCall staffCall = staffCallRepository.save(StaffCall.builder()
                .session(session)
                .reason("직원 호출")
                .build());
        staffCall.changeRequestedAt(requestedAt);
        return staffCallRepository.save(staffCall);
    }
}
