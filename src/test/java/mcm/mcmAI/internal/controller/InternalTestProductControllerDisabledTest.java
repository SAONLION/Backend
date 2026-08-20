package mcm.mcmAI.internal.controller;

import mcm.mcmAI.support.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InternalTestProductControllerDisabledTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 플래그가_꺼져있으면_랜덤_태그_스캔_엔드포인트는_404를_반환한다() throws Exception {
        mockMvc.perform(get("/internal/test/products/random-tag")
                        .param("sessionId", "550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(status().isNotFound());
    }
}
