package mcm.mcmAI.domain.email.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import mcm.mcmAI.domain.email.type.TriggerType;

@Schema(description = "개인화 추천 메일 발송 요청")
public record EmailSendRequest(

        @Email(message = "이메일 형식을 확인해주세요.")
        @Schema(description = "메일을 받을 주소", example = "guest@example.com")
        String email,

        @Schema(description = "마케팅 수신 동의 여부. false면 발송하지 않고 400(MARKETING_CONSENT_REQUIRED)을 반환한다.",
                example = "true")
        boolean consentMarketing,

        @Schema(description = "발송 트리거. 생략하면 SESSION_END로 간주한다.", example = "SESSION_END", nullable = true)
        TriggerType triggerType,

        @Schema(description = "메일 언어. 생략하면 세션의 language를 따르고, 그것도 없으면 ko다.",
                example = "ko", nullable = true)
        String language
) {
}
