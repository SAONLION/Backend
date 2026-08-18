package mcm.mcmAI.domain.qna.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "QnA 질문 요청")
public record QnaRequest(

        @NotBlank
        @Schema(
                description = "문의 유형",
                example = "FREE_TEXT",
                allowableValues = {"AS_REPAIR", "CARE", "GIFT_WRAP", "TAX_REFUND", "SHIPPING_RETURN", "FREE_TEXT"}
        )
        String questionType,

        @Schema(description = "자유 질문 내용 (questionType이 FREE_TEXT일 때만 필수)", example = "비 오는 날 들어도 괜찮을까요?")
        String question
) {
}
