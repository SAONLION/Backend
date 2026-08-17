package mcm.mcmAI.domain.tryonrequest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import mcm.mcmAI.domain.tryonrequest.entity.TryonRequest;

@Schema(description = "착장 요청 응답")
public record TryonRequestResponse(

        @Schema(description = "착장 요청 ID", example = "1")
        Long tryonRequestId,

        @Schema(description = "SKU", example = "9")
        Long sku,

        @Schema(description = "사이즈", example = "S-M")
        String size,

        @Schema(description = "컬러", example = "베이지")
        String color,

        @Schema(description = "요청 시각")
        LocalDateTime requestedAt
) {

    public static TryonRequestResponse from(TryonRequest tryonRequest) {
        return new TryonRequestResponse(
                tryonRequest.getTryonRequestId(),
                tryonRequest.getSku().getSku(),
                tryonRequest.getSize(),
                tryonRequest.getColor(),
                tryonRequest.getRequestedAt()
        );
    }
}
