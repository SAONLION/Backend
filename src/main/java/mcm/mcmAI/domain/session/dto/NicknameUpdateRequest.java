package mcm.mcmAI.domain.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "닉네임 변경 요청")
public record NicknameUpdateRequest(

        @NotBlank
        @Size(min = 1, max = 20)
        @Schema(description = "닉네임 (1~20자)", example = "손님1")
        String nickname
) {
}