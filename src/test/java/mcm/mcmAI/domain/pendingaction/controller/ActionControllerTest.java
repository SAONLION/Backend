package mcm.mcmAI.domain.pendingaction.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import mcm.mcmAI.domain.pendingaction.entity.PendingAction;
import mcm.mcmAI.domain.pendingaction.entity.PendingActionOption;
import mcm.mcmAI.domain.pendingaction.repository.PendingActionRepository;
import mcm.mcmAI.domain.pendingaction.type.ActionNextStep;
import mcm.mcmAI.domain.pendingaction.type.BlockerType;
import mcm.mcmAI.domain.pendingaction.type.PendingActionStatus;
import mcm.mcmAI.domain.session.entity.Session;
import mcm.mcmAI.domain.session.repository.SessionRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private PendingActionRepository pendingActionRepository;

    @Test
    void 옵션키로_응답하면_해당_옵션의_actionNextStep을_반환하고_상태가_RESPONDED로_바뀐다() throws Exception {
        PendingAction pendingAction = pendingActionRepository.save(PendingAction.builder()
                .session(newSession())
                .blockerType(BlockerType.CB5)
                .popupTitle("관심있어 하시던 백팩")
                .popupBody("비 오는 날에도 젖지 않는다는 것, 알고 계셨나요?")
                .options(List.of(
                        new PendingActionOption("ask_staff", "궁금해요, 직원에게 더 물어보기", ActionNextStep.STAFF_CALL_CREATED),
                        new PendingActionOption("other_product", "관심없어요 / 다른 제품 보기", ActionNextStep.SHOW_RECOMMENDATIONS)
                ))
                .build());

        mockMvc.perform(post("/api/v1/actions/{actionId}/respond", pendingAction.getActionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(respondBody("ask_staff")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionId").value(pendingAction.getActionId()))
                .andExpect(jsonPath("$.recordedResponse").value("ask_staff"))
                .andExpect(jsonPath("$.actionNextStep").value("STAFF_CALL_CREATED"))
                .andExpect(jsonPath("$.result").doesNotExist());

        PendingAction saved = pendingActionRepository.findById(pendingAction.getActionId()).orElseThrow();
        Assertions.assertEquals(PendingActionStatus.RESPONDED, saved.getStatus());
    }

    @Test
    void dismissed는_옵션목록에_없어도_항상_유효하고_NONE을_반환한다() throws Exception {
        PendingAction pendingAction = pendingActionRepository.save(PendingAction.builder()
                .session(newSession())
                .blockerType(BlockerType.CB3)
                .popupTitle("직원에게 직접 안내를 받아보시겠어요?")
                .options(List.of(
                        new PendingActionOption("ask_staff", "네, 불러주세요", ActionNextStep.STAFF_CALL_CREATED),
                        new PendingActionOption("dismiss", "괜찮아요", ActionNextStep.NONE)
                ))
                .build());

        mockMvc.perform(post("/api/v1/actions/{actionId}/respond", pendingAction.getActionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(respondBody("dismissed")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordedResponse").value("dismissed"))
                .andExpect(jsonPath("$.actionNextStep").value("NONE"));
    }

    @Test
    void CB1_check_other_store_옵션은_STOCK_REQUEST_COMPLETED와_더미_result를_반환한다() throws Exception {
        PendingAction pendingAction = pendingActionRepository.save(PendingAction.builder()
                .session(newSession())
                .blockerType(BlockerType.CB1)
                .popupTitle("찾으시는 컬러는 현재 이 매장에 재고가 없습니다")
                .options(List.of(
                        new PendingActionOption("check_other_store", "가까운 매장 홀딩 요청", ActionNextStep.STOCK_REQUEST_COMPLETED)
                ))
                .build());

        mockMvc.perform(post("/api/v1/actions/{actionId}/respond", pendingAction.getActionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(respondBody("check_other_store")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionNextStep").value("STOCK_REQUEST_COMPLETED"))
                .andExpect(jsonPath("$.result.message").value("인근 매장 재고를 확인해드렸어요."))
                .andExpect(jsonPath("$.result.storeName").value("MCM 신세계 강남점"))
                .andExpect(jsonPath("$.result.stock").value(true));
    }

    @Test
    void 존재하지_않는_actionId면_404와_ACTION_NOT_FOUND_코드를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/actions/{actionId}/respond", 999_999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(respondBody("ask_staff")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACTION_NOT_FOUND"));
    }

    @Test
    void 옵션목록에도_없고_dismissed도_아니면_400과_INVALID_RESPONSE_KEY_코드를_반환한다() throws Exception {
        PendingAction pendingAction = pendingActionRepository.save(PendingAction.builder()
                .session(newSession())
                .blockerType(BlockerType.CB6)
                .popupTitle("관심있던 제품에 대한 콘텐츠를 받아보시겠어요?")
                .options(List.of(
                        new PendingActionOption("content_offer_accept", "네, 알려주세요", ActionNextStep.SHOW_EMAIL_FORM),
                        new PendingActionOption("content_offer_decline", "괜찮아요", ActionNextStep.NONE)
                ))
                .build());

        mockMvc.perform(post("/api/v1/actions/{actionId}/respond", pendingAction.getActionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(respondBody("not_a_real_key")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESPONSE_KEY"));
    }

    private Session newSession() {
        return sessionRepository.save(Session.builder()
                .sessionId(UUID.randomUUID().toString())
                .language("ko")
                .build());
    }

    private String respondBody(String responseKey) {
        return "{\"responseKey\":\"" + responseKey + "\"}";
    }
}