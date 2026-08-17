package mcm.mcmAI.domain.purchaseinquiry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.purchaseinquiry.dto.PurchaseInquiryRequest;
import mcm.mcmAI.domain.purchaseinquiry.dto.PurchaseInquiryResponse;
import mcm.mcmAI.domain.purchaseinquiry.service.PurchaseInquiryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "PurchaseInquiry", description = "구매 문의 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sessions/{sessionId}/purchase-inquiries")
public class PurchaseInquiryController {

    private final PurchaseInquiryService purchaseInquiryService;

    @Operation(
            summary = "구매 문의",
            description = "착장 요청 완료 화면에서 '구매 문의' 버튼 클릭 시 호출한다. 구매 의사를 명시적으로 표현하는 "
                    + "이벤트로 intent-score 산정 시 가장 중요하게 반영될 예정이다. 세션이 존재하지 않으면 "
                    + "404(SESSION_NOT_FOUND), sku가 존재하지 않으면 404(SKU_NOT_FOUND)를 반환한다."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseInquiryResponse createPurchaseInquiry(
            @Parameter(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String sessionId,

            @Valid @RequestBody PurchaseInquiryRequest request
    ) {
        return purchaseInquiryService.createPurchaseInquiry(sessionId, request);
    }
}
