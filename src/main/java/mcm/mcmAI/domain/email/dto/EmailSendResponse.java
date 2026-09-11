package mcm.mcmAI.domain.email.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import mcm.mcmAI.domain.email.entity.PotentialCustomer;
import mcm.mcmAI.domain.email.type.SentStatus;

@Schema(description = "개인화 추천 메일 발송 응답")
public record EmailSendResponse(

        @Schema(description = "잠재고객 ID", example = "1")
        Long pcId,

        @Schema(description = "수신 주소", example = "guest@example.com")
        String email,

        @Schema(description = "발송 상태. 전송에 실패해도 이력은 FAILED로 남기고 200을 반환한다.", example = "SENT")
        SentStatus sentStatus,

        @Schema(description = "발송 시각. 실패했으면 null이다.", nullable = true)
        LocalDateTime sentAt,

        @Schema(description = "메일에 실린 PICK 개수 (0~4)", example = "3")
        int filledPickCount,

        @Schema(description = "메일에 실린 추천 상품 개수 (0~2)", example = "2")
        int filledRecommendCount
) {

    public static EmailSendResponse of(PotentialCustomer customer, int filledPickCount, int filledRecommendCount) {
        return new EmailSendResponse(
                customer.getPcId(), customer.getEmail(), customer.getSentStatus(), customer.getSentAt(),
                filledPickCount, filledRecommendCount
        );
    }
}
