package mcm.mcmAI.domain.contact.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

@Schema(description = "콘텐츠 수신 연락처 등록 요청")
public record ContactRequest(

        @Schema(description = "직전 응답한 PendingAction의 actionId", example = "9001", nullable = true)
        Long actionId,

        @Schema(description = "제품 ID", example = "101", nullable = true)
        Long productId,

        @Email(message = "이메일 형식을 확인해주세요.")
        @Schema(description = "콘텐츠를 받을 이메일", example = "guest@example.com")
        String email,

        @Schema(description = "발송할 콘텐츠 주제", example = "Care & Styling Content", nullable = true)
        String contentTopic
) {
}