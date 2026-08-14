package mcm.mcmAI.domain.visitpurpose.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "방문 목적 등록 요청")
public record VisitPurposeRequest(

        @NotBlank
        @Schema(description = "방문 목적 타입", example = "TRAVEL")
        String purposeType
) {
}