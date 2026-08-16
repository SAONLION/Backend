package mcm.mcmAI.domain.contact.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.contact.dto.ContactRequest;
import mcm.mcmAI.domain.contact.dto.ContactResponse;
import mcm.mcmAI.domain.contact.service.ContactService;
import mcm.mcmAI.global.exception.ErrorCode;
import mcm.mcmAI.global.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Contact", description = "CB5/CB6 콘텐츠 수신 연락처 등록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/session/contacts")
public class ContactController {

    private final ContactService contactService;

    @Operation(
            summary = "콘텐츠 수신 연락처 등록",
            description = "CB5/CB6 팝업에서 콘텐츠 수신에 동의한 고객의 이메일을 저장한다. MVP의 핵심 성과 지표인 "
                    + "연락 가능한 접점(이메일) 확보를 위한 API다. 실제 SMTP 발송은 하지 않으며, 로그만 남기고 "
                    + "contentSent=true, sentAt=현재시각으로 응답한다. actionId/productId/contentTopic은 "
                    + "선택값이다. 세션이 존재하지 않으면 404(SESSION_NOT_FOUND), email이 비어있으면 "
                    + "400(MISSING_CONTACT_INFO), email 형식이 올바르지 않으면 400(INVALID_EMAIL)을 반환한다."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactResponse createContact(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String sessionId,

            @Valid @RequestBody ContactRequest request
    ) {
        return contactService.createContact(sessionId, request);
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