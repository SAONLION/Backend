package mcm.mcmAI.domain.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.product.dto.HubOptionResponse;
import mcm.mcmAI.domain.product.dto.ProductTagScanResponseDTO;
import mcm.mcmAI.domain.product.dto.SubOptionDTO;
import mcm.mcmAI.domain.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Product", description = "상품/태그 스캔 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    @Operation(
            summary = "태그 스캔",
            description = "NFC/QR 태그(SKU) 스캔 시 상품 정보와 1차 허브 옵션 4종(PRODUCT_UNDERSTANDING, FIT_PREFERENCE, "
                    + "PURCHASE_CONDITION, OTHER)을 반환한다. 스캔 시점에 tag_scan_log에 세션 기준 스캔 기록을 남긴다."
    )
    @GetMapping("/tags/{tagId}")
    public ProductTagScanResponseDTO scanTag(
            @Parameter(description = "스캔된 태그(SKU) ID", example = "1")
            @PathVariable Long tagId,

            @Parameter(description = "스캔을 수행한 세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String sessionId
    ) {
        return productService.getProductByTag(tagId, sessionId);
    }

    @Operation(
            summary = "2차 허브 세부 옵션 조회",
            description = "1차 허브에서 선택한 interestType에 해당하는 2차 세부 옵션 목록을 반환한다. "
                    + "interestType이 4종(PRODUCT_UNDERSTANDING, FIT_PREFERENCE, PURCHASE_CONDITION, OTHER) 중 "
                    + "하나가 아니면 400 에러를 반환한다."
    )
    @GetMapping("/{productId}/hub")
    public List<SubOptionDTO> getHubOptions(
            @Parameter(description = "상품 ID", example = "1")
            @PathVariable Long productId,

            @Parameter(description = "1차 허브에서 선택한 관심사 타입", example = "PRODUCT_UNDERSTANDING")
            @RequestParam String interestType
    ) {
        return productService.getHubOptions(productId, interestType);
    }

    @Operation(
            summary = "허브 하위 옵션 상세 조회",
            description = "2차 허브 옵션 목록 중 하나를 클릭했을 때 상세 내용을 반환한다. 옵션 타입이 INFO면 content를 "
                    + "바로 노출하고, STAFF_MEDIATED면 실제 값(특히 가격) 대신 다음 단계 안내를 반환한다 — 가격은 "
                    + "절대 직접 노출하지 않으며 SA가 대시보드에서 안내한다. productId 또는 optionId가 존재하지 "
                    + "않으면 각각 PRODUCT_NOT_FOUND, OPTION_NOT_FOUND 코드와 함께 404를 반환한다."
    )
    @GetMapping("/{productId}/hub/options/{optionId}")
    public HubOptionResponse getHubOptionDetail(
            @Parameter(description = "상품 ID", example = "1")
            @PathVariable Long productId,

            @Parameter(description = "2차 허브 옵션 조회 응답(options[].id)의 하위 옵션 ID", example = "9")
            @PathVariable String optionId
    ) {
        return productService.getHubOptionDetail(productId, optionId);
    }
}