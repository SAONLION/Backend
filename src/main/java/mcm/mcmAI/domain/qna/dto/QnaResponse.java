package mcm.mcmAI.domain.qna.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "QnA 답변 응답")
public record QnaResponse(

        @Schema(description = "답변 내용")
        String answer
) {

    public static QnaResponse of(String answer) {
        return new QnaResponse(answer);
    }
}
