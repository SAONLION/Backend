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
                    + "가격/재고 정보는 포함하지 않는다.\n\n"
                    + "[P2-17] images는 문자열 URL 배열(images: string[])에서 객체 배열로 구조가 바뀌었다 "
                    + "(images: {url, shotType, hasPerson}[]). shotType은 PRODUCT(제품컷)/MODEL(모델컷) "
                    + "중 하나이며, 화면에서 제품컷/모델컷을 나누는 기준 필드다. hasPerson은 참고용 보조 필드로 "
                    + "판별 기준으로 쓰지 않는다. 모델컷이 없는 SKU는 images가 빈 배열이다. 하위 호환을 깨는 "
                    + "변경이므로, 기존 images: string[]을 그대로 소비하던 프론트는 이 필드를 objects 배열로 "
                    + "파싱하도록 전환해야 한다(url을 기존 문자열 자리에 사용).\n\n"
                    + "[P2-20] sizeOptions가 추가됐다({code, dimensions}[], size와 같은 순서). 기존 size/dimensions는 "
                    + "하위 호환을 위해 그대로 유지된다. 원본 데이터가 SKU당 치수 값을 하나만 보유하고 있어, size에 "
                    + "사이즈 코드가 2개 이상이면 그중 어느 코드의 실측치인지 확정할 수 없다 — 이 경우 sizeOptions의 "
                    + "모든 dimensions는 null이며(같은 값을 여러 사이즈에 추정해 채우지 않는다), 기존 dimensions 필드에는 "
                    + "여전히 원본의 단일 값이 내려간다. 사이즈 코드가 하나뿐이면 그 dimensions 값이 곧 그 사이즈의 "
                    + "실측치이므로 sizeOptions에도 동일하게 채워진다."
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