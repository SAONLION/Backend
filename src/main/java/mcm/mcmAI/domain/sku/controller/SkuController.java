package mcm.mcmAI.domain.sku.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mcm.mcmAI.domain.sku.dto.SkuDetailResponse;
import mcm.mcmAI.domain.sku.dto.SkuListItemResponse;
import mcm.mcmAI.domain.sku.service.SkuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sku", description = "SKU(색상/사이즈) 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products/{productId}/skus")
public class SkuController {

    private final SkuService skuService;

    @Operation(
            summary = "색상별 SKU 목록 조회",
            description = "해당 상품의 색상별 SKU 목록을 반환한다. 화면의 컬러 탭 전환에 사용된다. "
                    + "가격/재고 정보는 포함하지 않는다."
    )
    @GetMapping
    public List<SkuListItemResponse> getSkus(
            @Parameter(description = "상품 ID", example = "1")
            @PathVariable Long productId
    ) {
        return skuService.getSkus(productId);
    }

    @Operation(
            summary = "SKU 상세 조회",
            description = "특정 SKU의 상세 정보와 이미지 목록을 반환한다. 화면의 사이즈 확인에 사용된다. "
                    + "가격/재고 정보는 포함하지 않는다."
    )
    @GetMapping("/{skuId}")
    public SkuDetailResponse getSkuDetail(
            @Parameter(description = "상품 ID", example = "1")
            @PathVariable Long productId,

            @Parameter(description = "SKU ID", example = "1")
            @PathVariable Long skuId
    ) {
        return skuService.getSkuDetail(productId, skuId);
    }
}