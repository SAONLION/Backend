package mcm.mcmAI.domain.qna.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.qna.dto.QnaRequest;
import mcm.mcmAI.domain.qna.dto.QnaResponse;
import mcm.mcmAI.domain.qna.service.QnaService;
import mcm.mcmAI.global.aop.RequiresActiveSession;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Qna", description = "자유 형식 질문 응답 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products/{productId}/qna")
public class QnaController {

    private final QnaService qnaService;

    @Operation(
            summary = "QnA 질문 답변",
            description = "\"선택지에 없는 게 궁금하시면\" 화면에서 사용한다. AS_REPAIR/CARE/GIFT_WRAP/TAX_REFUND는 "
                    + "고정 답변을, SHIPPING_RETURN은 세션 language별 고정 답변을 AI 호출 없이 반환한다. FREE_TEXT는 "
                    + "제품 정보를 컨텍스트로 AI가 실시간 답변을 생성하며, AI 호출에 실패하면 fallback 답변을 반환한다. "
                    + "language는 세션에 저장된 값을 자동으로 사용한다. 세션이 존재하지 않으면 404(SESSION_NOT_FOUND), "
                    + "productId가 존재하지 않는 제품이면 404(PRODUCT_NOT_FOUND)를 반환한다."
    )
    @RequiresActiveSession
    @PostMapping
    public QnaResponse ask(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String sessionId,

            @Parameter(description = "상품 ID", example = "1")
            @PathVariable Long productId,

            @Valid @RequestBody QnaRequest request
    ) {
        return qnaService.answer(productId, sessionId, request);
    }
}
