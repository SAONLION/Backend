package mcm.mcmAI.domain.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequestDTO(

        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Size(max = 50)
        String category,

        String materialDesc,

        String heritageDesc
) {
}