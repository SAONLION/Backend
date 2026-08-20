package mcm.mcmAI.domain.journeycard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "세션에서 가장 많이 태그 스캔된 색상")
public record FavoriteColorResponse(

        @Schema(description = "서버의 안정적인 색상 식별값 (SKU color 값을 대문자로 정규화). "
                + "고정 코드 체계가 없어 label을 정규화해 생성한다.", example = "COGNAC")
        String code,

        @Schema(description = "고객 화면 표시용 색상명", example = "Cognac")
        String label,

        @Schema(description = "계산 근거 확인용 태그 스캔 횟수. 고객 UI에 노출할 필요는 없다.", example = "3")
        int tagCount
) {
}
