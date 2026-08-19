package mcm.mcmAI.domain.qna.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "QnA 답변 응답")
public record QnaResponse(

        @Schema(description = "답변 내용")
        String answer,

        @Schema(description = "컨텍스트 내에서 실질적으로 답변했는지 여부")
        boolean resolved
) {

    public static QnaResponse of(String answer, boolean resolved) {
        return new QnaResponse(answer, resolved);
    }
}
