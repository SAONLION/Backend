package mcm.mcmAI.domain.email.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 메일 본문에 필요하지만 ERD에서 얻을 수 없는 값들(매장명, CDN 주소, 푸터 링크 등)의 설정 묶음.
 */
@Getter
@Component
public class EmailProperties {

    private final String templatePath;
    private final String assetBaseUrl;
    private final String placeholderImageUrl;
    private final String storeName;
    private final String defaultNickname;
    private final String unsubscribeUrl;
    private final String privacyUrl;
    private final String fromAddress;
    private final String subjectFormat;

    public EmailProperties(
            @Value("${app.email.template-path}") String templatePath,
            @Value("${app.email.asset-base-url}") String assetBaseUrl,
            @Value("${app.email.placeholder-image-url}") String placeholderImageUrl,
            @Value("${app.email.store-name}") String storeName,
            @Value("${app.email.default-nickname}") String defaultNickname,
            @Value("${app.email.unsubscribe-url}") String unsubscribeUrl,
            @Value("${app.email.privacy-url}") String privacyUrl,
            @Value("${app.email.from-address}") String fromAddress,
            @Value("${app.email.subject-format}") String subjectFormat
    ) {
        this.templatePath = templatePath;
        // 템플릿에서 "{{assetBaseUrl}}/bg-main.png" 형태로 이어 붙이므로 끝의 /는 제거해 //를 만들지 않는다.
        this.assetBaseUrl = stripTrailingSlash(assetBaseUrl);
        this.placeholderImageUrl = placeholderImageUrl;
        this.storeName = storeName;
        this.defaultNickname = defaultNickname;
        this.unsubscribeUrl = unsubscribeUrl;
        this.privacyUrl = privacyUrl;
        this.fromAddress = fromAddress;
        this.subjectFormat = subjectFormat;
    }

    /** 제목 형식 문자열의 %s 자리에 닉네임을 채운다. */
    public String buildSubject(String nickname) {
        return String.format(subjectFormat, nickname);
    }

    private static String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
