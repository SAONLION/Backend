package mcm.mcmAI.domain.contact.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import mcm.mcmAI.domain.contact.entity.Contact;

@Schema(description = "콘텐츠 수신 연락처 등록 응답")
public record ContactResponse(

        @Schema(description = "연락처 ID", example = "1")
        Long contactId,

        @Schema(description = "콘텐츠 발송 여부", example = "true")
        boolean contentSent,

        @Schema(description = "발송 시각")
        LocalDateTime sentAt
) {

    public static ContactResponse from(Contact contact) {
        return new ContactResponse(contact.getContactId(), contact.isContentSent(), contact.getSentAt());
    }
}