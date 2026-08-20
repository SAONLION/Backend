package mcm.mcmAI.domain.sku.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import mcm.mcmAI.domain.sku.entity.Sku;

@Schema(description = "SKU 상세 정보 (가격/재고 미포함)")
public record SkuDetailResponse(

        @Schema(description = "SKU ID", example = "1")
        Long skuId,

        @Schema(description = "색상", example = "Black")
        String color,

        @Schema(description = "사이즈(콤마로 구분된 선택 가능 사이즈 목록)", example = "XS,S,M,L,XL")
        String size,

        @Schema(description = "이미지 목록 (정렬 순서대로). 모델컷이 없는 SKU는 빈 배열이다. "
                + "구 버전(images: string[])과 호환되지 않는 응답 구조 변경이다.")
        List<SkuImageResponse> images,

        @Schema(description = "가로/세로/폭 치수 안내 (값이 없으면 null)", example = "약 11 x 33 x 31 센티미터", nullable = true)
        String dimensions,

        @Schema(description = "포켓/수납 관련 안내 (값이 없으면 null)", nullable = true)
        String storage,

        @Schema(description = "스트랩 길이/핸들 드롭 안내 (값이 없으면 null)", nullable = true)
        String strap
) {

    public static SkuDetailResponse of(Sku sku, List<SkuImageResponse> images) {
        return new SkuDetailResponse(
                sku.getSku(), sku.getColor(), sku.getSize(), images,
                sku.getDimensionsText(), sku.getStorageText(), strapOf(sku)
        );
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