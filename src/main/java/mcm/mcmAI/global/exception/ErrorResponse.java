package mcm.mcmAI.global.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Schema(description = "에러 응답")
public record ErrorResponse(

        @Schema(description = "에러 발생 시각")
        OffsetDateTime timestamp,

        @Schema(description = "HTTP 상태 코드", example = "404")
        int status,

        @Schema(description = "HTTP 상태 텍스트", example = "Not Found")
        String error,

        @Schema(description = "에러 코드", example = "OPTION_NOT_FOUND")
        String code,

        @Schema(description = "에러 메시지", example = "해당 옵션을 찾을 수 없습니다.")
        String message,

        @Schema(description = "요청 경로", example = "/api/v1/products/1/hub/options/opt_999")
        String path
) {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return new ErrorResponse(
                OffsetDateTime.now(ZONE),
                errorCode.getStatus().value(),
                errorCode.getStatus().getReasonPhrase(),
                errorCode.name(),
                errorCode.getMessage(),
                path
        );
    }
}