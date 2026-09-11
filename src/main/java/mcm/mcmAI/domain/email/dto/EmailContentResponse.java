package mcm.mcmAI.domain.email.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "email_content.html 템플릿 치환에 필요한 데이터 일습")
public record EmailContentResponse(

        @Schema(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "{{nickname}}. 세션에 닉네임이 없으면 기본 호칭(app.email.default-nickname)으로 대체된다.",
                example = "고객님")
        String nickname,

        @Schema(description = "{{storeName}}. ERD에 매장 정보가 없어 app.email.store-name 설정값을 사용한다.",
                example = "청담 MCM HAUS")
        String storeName,

        @Schema(description = "PICK 슬롯 4개. tag_scan_log의 최근 태그 순으로 채우며, 태그 이력이 4개보다 적으면 "
                + "뒤쪽 슬롯은 skuId가 null인 빈 슬롯이 된다. 템플릿 레이아웃이 고정 4칸이라 길이는 항상 4다.")
        List<EmailSlotItem> picks,

        @Schema(description = "추천 슬롯 2개. RecommendationService 결과 상위 2개다. 길이는 항상 2다.")
        List<EmailSlotItem> recommendations,

        @Schema(description = "실제로 채워진 PICK 개수 (0~4)", example = "3")
        int filledPickCount,

        @Schema(description = "{{assetBaseUrl}}. 로고·배경·매장 사진 등 고정 이미지의 절대 URL 접두사",
                example = "https://cdn.example.com/email")
        String assetBaseUrl,

        @Schema(description = "{{unsubscribeUrl}}", example = "https://api.tagonai.site/api/v1/email/unsubscribe?token=...")
        String unsubscribeUrl,

        @Schema(description = "{{privacyUrl}}", example = "https://tagonai.site/privacy")
        String privacyUrl
) {
}
