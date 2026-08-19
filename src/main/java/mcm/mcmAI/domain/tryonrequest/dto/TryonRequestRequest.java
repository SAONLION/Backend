package mcm.mcmAI.domain.tryonrequest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "착장 요청")
public record TryonRequestRequest(

        @NotNull
        @Schema(description = "착장을 요청할 SKU", example = "9")
        Long sku
) {
}