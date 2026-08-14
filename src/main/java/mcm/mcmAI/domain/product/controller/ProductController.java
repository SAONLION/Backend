package mcm.mcmAI.domain.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
                    + "PURCHASE_CONDITION, OTHER)을 반환한다."
    )
    @GetMapping("/tags/{tagId}")
    public ProductTagScanResponseDTO scanTag(
            @Parameter(description = "스캔된 태그(SKU) ID", example = "1")
            @PathVariable Long tagId
    ) {
        return productService.getProductByTag(tagId);
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
}