package mcm.mcmAI.domain.journeycard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "여정 카드(패스포트) 응답")
public record JourneyCardResponse(

        @Schema(description = "브랜드명", example = "MCM")
        String brand,

        @Schema(description = "세션 생성일 (YY/MM/DD)", example = "26/08/16")
        String date,

        @Schema(description = "세션에 저장된 닉네임", example = "mingyu")
        String nickname,

        @Schema(description = "세션 코드 (세션 ID 앞 5자). 형식은 추후 변경될 수 있다.", example = "d0571")
        String sessionCode,

        @Schema(description = "콜라주 이미지 목록 (관심도 상위 제품 순으로 최대 4장). "
                + "태그 이력이 없으면 빈 배열이다.")
        List<CollageImageResponse> collageImages,

        @Schema(description = "콜라주 4장이 모두 채워졌는지 여부. 프론트가 '채워졌어요' 팝업 노출 여부를 판단할 때 사용한다.",
                example = "true")
        boolean isComplete,

        @Schema(description = "세션 안에서 가장 많이 태그 스캔된 색상. 동률이면 가장 최근 태그한 색상이다. "
                + "태그 이력이 없으면 null이다.")
        FavoriteColorResponse favoriteColor
) {
}