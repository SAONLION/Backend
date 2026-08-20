package mcm.mcmAI.domain.sku.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.List;
import mcm.mcmAI.domain.sku.entity.Sku;

@Schema(description = "SKU 상세 정보 (가격/재고 미포함)")
public record SkuDetailResponse(

        @Schema(description = "SKU ID", example = "1")
        Long skuId,

        @Schema(description = "색상", example = "Black")
        String color,

        @Schema(description = "사이즈(콤마로 구분된 선택 가능 사이즈 목록). 하위 호환을 위해 유지하며, "
                + "사이즈별 치수는 sizeOptions를 사용한다.", example = "XS,S,M,L,XL")
        String size,

        @Schema(description = "이미지 목록 (정렬 순서대로). 모델컷이 없는 SKU는 빈 배열이다. "
                + "구 버전(images: string[])과 호환되지 않는 응답 구조 변경이다.")
        List<SkuImageResponse> images,

        @Schema(description = "[P2-20] 가로/세로/폭 치수 안내 (값이 없으면 null). size에 사이즈가 여러 개일 때도 "
                + "원본 데이터상 사이즈 구분 없이 대표값 하나만 있어, 실제로는 그중 한 사이즈의 치수일 수 있다 — "
                + "사이즈별로 정확히 구분된 값은 sizeOptions를 사용한다. 하위 호환을 위해 유지한다.",
                example = "약 11 x 33 x 31 센티미터", nullable = true)
        String dimensions,

        @Schema(description = "포켓/수납 관련 안내 (값이 없으면 null)", nullable = true)
        String storage,

        @Schema(description = "스트랩 길이/핸들 드롭 안내 (값이 없으면 null)", nullable = true)
        String strap,

        @Schema(description = "[P2-20] size 필드의 각 사이즈 코드별 치수 목록(size와 같은 순서). 사이즈가 하나뿐인 "
                + "SKU도 원소 1개짜리 배열로 내려와 프론트가 항상 같은 구조로 소비할 수 있다. 원본 데이터가 SKU당 "
                + "치수 값을 하나만 보유해 사이즈가 2개 이상이면 그중 어느 코드의 실측치인지 확정할 수 없으므로, "
                + "이 경우 모든 항목의 dimensions는 null이다(같은 값을 여러 사이즈에 추정해 채우지 않는다). "
                + "size가 비어 있으면 빈 배열이다.")
        List<SkuSizeOptionResponse> sizeOptions
) {

    public static SkuDetailResponse of(Sku sku, List<SkuImageResponse> images) {
        return new SkuDetailResponse(
                sku.getSku(), sku.getColor(), sku.getSize(), images,
                sku.getDimensionsText(), sku.getStorageText(), strapOf(sku), sizeOptionsOf(sku)
        );
    }

    private static List<SkuSizeOptionResponse> sizeOptionsOf(Sku sku) {
        String size = sku.getSize();
        if (size == null || size.isBlank()) {
            return List.of();
        }

        List<String> codes = Arrays.stream(size.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .toList();

        // 사이즈 코드가 하나뿐이면 SKU의 단일 dimensionsText가 그 사이즈의 실측치임이 확실하다.
        // 2개 이상이면 원본에 사이즈별 구분이 없어 어느 코드의 값인지 확정할 수 없으므로 전부 null로 내린다.
        boolean singleSize = codes.size() == 1;
        return codes.stream()
                .map(code -> new SkuSizeOptionResponse(code, singleSize ? sku.getDimensionsText() : null))
                .toList();
    }

    private static String strapOf(Sku sku) {
        String strapLength = sku.getStrapLength();
        String handleDrop = sku.getHandleDrop();
        boolean hasStrapLength = strapLength != null && !strapLength.isBlank();
        boolean hasHandleDrop = handleDrop != null && !handleDrop.isBlank();

        if (hasStrapLength && hasHandleDrop) {
            return "스트랩 길이 %s · 핸들 드롭 %s".formatted(strapLength, handleDrop);
        }
        if (hasStrapLength) {
            return "스트랩 길이 %s".formatted(strapLength);
        }
        if (hasHandleDrop) {
            return "핸들 드롭 %s".formatted(handleDrop);
        }
        return null;
    }
}