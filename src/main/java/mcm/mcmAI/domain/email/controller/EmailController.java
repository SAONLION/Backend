package mcm.mcmAI.domain.email.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.email.dto.EmailContentResponse;
import mcm.mcmAI.domain.email.dto.EmailSendRequest;
import mcm.mcmAI.domain.email.dto.EmailSendResponse;
import mcm.mcmAI.domain.email.service.PotentialCustomerEmailService;
import mcm.mcmAI.global.aop.RequiresActiveSession;
import mcm.mcmAI.global.exception.ErrorCode;
import mcm.mcmAI.global.exception.ErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Email", description = "개인화 추천 메일 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/session/email")
public class EmailController {

    private final PotentialCustomerEmailService potentialCustomerEmailService;

    @Operation(
            summary = "메일 콘텐츠 조회",
            description = "email_content.html의 치환 토큰에 들어갈 값을 JSON으로 반환한다. 닉네임은 session, "
                    + "PICK 4칸은 tag_scan_log의 최근 태그 4건(중복 SKU 제거 후 태그 순서대로), 추천 2칸은 "
                    + "추천 API와 동일한 로직의 상위 2건에서 가져온다. 상품 이미지는 sku_image의 PRODUCT 샷 "
                    + "중 position이 가장 앞선 것을 쓰고, 없으면 placeholder URL로 채운다. 매장명·푸터 링크는 "
                    + "ERD에 없는 값이라 app.email.* 설정을 따른다. 태그 이력이 4건보다 적거나 태그한 상품이 "
                    + "이후 삭제됐으면 해당 슬롯은 skuId가 null인 빈 슬롯이 되며, picks의 길이는 레이아웃 유지를 "
                    + "위해 항상 4(recommendations는 2)다. 세션이 없으면 404(SESSION_NOT_FOUND)를 반환한다."
    )
    @GetMapping("/content")
    public EmailContentResponse getContent(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String sessionId
    ) {
        return potentialCustomerEmailService.getContent(sessionId);
    }

    @Operation(
            summary = "메일 HTML 미리보기",
            description = "GET /content와 똑같은 데이터로 템플릿을 렌더링한 최종 HTML을 그대로 반환한다. "
                    + "발송 전 QA·디자인 확인용이며 발송 이력(potential_customer)은 남기지 않는다. "
                    + "템플릿 파일을 읽지 못하면 500(EMAIL_TEMPLATE_NOT_FOUND)을 반환한다."
    )
    @GetMapping(value = "/preview", produces = MediaType.TEXT_HTML_VALUE)
    public String preview(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String sessionId
    ) {
        return potentialCustomerEmailService.getRenderedHtml(sessionId);
    }

    @Operation(
            summary = "개인화 추천 메일 발송",
            description = "수신자를 potential_customer에, 메일에 실린 상품 슬롯을 potential_customer_product에 "
                    + "기록한다. 실제 발송(app.email.sender=smtp일 때 MimeMessage 기반 MailService, 그 외엔 "
                    + "로그만 남기는 LoggingEmailSender)은 이 요청의 DB 트랜잭션이 커밋된 '이후' 비동기로 "
                    + "트리거되므로, 이 응답의 sentStatus는 실제 전송 결과와 무관하게 항상 PENDING이다. "
                    + "최종 성공/실패(SENT/FAILED)는 GET /status로 다시 조회해야 한다. 발송이 실패해도 "
                    + "요청 자체를 실패시키지 않고 이력만 FAILED로 남긴다(재시도·모니터링을 위함). "
                    + "세션이 없으면 404(SESSION_NOT_FOUND), 이미 종료된 세션이면 409(SESSION_ALREADY_ENDED), "
                    + "email이 비어 있으면 400(MISSING_CONTACT_INFO), 형식이 틀리면 400(INVALID_EMAIL), "
                    + "마케팅 수신에 동의하지 않았으면 400(MARKETING_CONSENT_REQUIRED)을 반환한다."
    )
    @RequiresActiveSession
    @PostMapping("/send")
    public EmailSendResponse send(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String sessionId,

            @Valid @RequestBody EmailSendRequest request
    ) {
        return potentialCustomerEmailService.send(sessionId, request);
    }

    @Operation(
            summary = "메일 발송 상태 조회",
            description = "POST /send가 발급한 pcId로 최종 발송 상태(PENDING/SENT/FAILED)를 조회한다. "
                    + "발송은 비동기로 처리되므로, 발송 직후에는 PENDING일 수 있고 잠시 후 다시 조회하면 "
                    + "SENT 또는 FAILED로 바뀐다. 해당 pcId가 없으면 404(POTENTIAL_CUSTOMER_NOT_FOUND)를 반환한다."
    )
    @GetMapping("/status")
    public EmailSendResponse getStatus(
            @Parameter(description = "잠재고객 ID (POST /send 응답의 pcId)", example = "1")
            @RequestParam Long pcId
    ) {
        return potentialCustomerEmailService.getStatus(pcId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEmail(
            MethodArgumentNotValidException e, HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_EMAIL;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }
}
