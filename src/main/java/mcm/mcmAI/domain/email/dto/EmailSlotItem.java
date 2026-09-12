package mcm.mcmAI.domain.email.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import mcm.mcmAI.domain.email.type.SlotType;

@Schema(description = "메일 템플릿의 상품 슬롯 하나. PICK 4칸과 추천 2칸이 같은 형태를 쓴다.")
public record EmailSlotItem(

        @Schema(description = "슬롯 종류", example = "PICK")
        SlotType slotType,

        @Schema(description = "슬롯 순번. PICK은 1~4, 추천은 1~2이며 템플릿의 {{pickName1}} 같은 토큰 번호와 일치한다.",
                example = "1")
        int slotOrder,

        @Schema(description = "SKU ID. 슬롯을 채우지 못했으면 null이다.", example = "1001", nullable = true)
        Long skuId,

        @Schema(description = "상품 ID. 슬롯을 채우지 못했으면 null이다.", example = "101", nullable = true)
        Long productId,

        @Schema(description = "상품명. 템플릿의 {{pickNameN}}/{{recommendNameN}}에 들어간다. 빈 슬롯이면 빈 문자열이다.",
                example = "비세토스 백팩 M 코냑")
        String productName,

        @Schema(description = "상품 설명. product.material_desc가 있으면 그 값을, 없으면 heritage_desc를 쓴다. "
                + "템플릿의 {{recommendDescN}}에 들어간다. 빈 슬롯이거나 둘 다 없으면 빈 문자열이다.",
                example = "사용한 어망을 혁신적으로 재생한 ECONYL® 리사이클 나일론 소재로 제작되었습니다.")
        String description,

        @Schema(description = "이미지 절대 URL. 템플릿의 {{pickImageUrlN}}/{{recommendImageUrlN}}에 들어간다. "
                + "등록된 이미지가 없거나 빈 슬롯이면 placeholder URL로 채워진다.",
                example = "https://cdn.example.com/sku/1001/product-1.jpg")
        String imageUrl
) {

    public static EmailSlotItem empty(SlotType slotType, int slotOrder, String placeholderImageUrl) {
        return new EmailSlotItem(slotType, slotOrder, null, null, "", "", placeholderImageUrl);
    }

    public boolean isFilled() {
        return skuId != null;
    }
}
