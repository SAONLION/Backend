package mcm.mcmAI.domain.email.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcm.mcmAI.domain.email.dto.EmailContentResponse;
import mcm.mcmAI.domain.email.dto.EmailSlotItem;
import mcm.mcmAI.global.exception.BusinessException;
import mcm.mcmAI.global.exception.ErrorCode;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * email_content.html의 {{token}}을 실제 값으로 치환한다.
 * 템플릿은 인라인 CSS만 쓰는 메일용 HTML이라 별도 템플릿 엔진 없이 단순 치환으로 충분하고,
 * 엔진을 끼우지 않는 편이 디자이너가 준 원본 HTML을 그대로 유지하기에도 좋다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailTemplateRenderer {

    private final ResourceLoader resourceLoader;
    private final EmailProperties emailProperties;

    /** 템플릿은 배포 후 바뀌지 않으므로 첫 렌더링 때 한 번만 읽어 캐시한다. */
    private volatile String cachedTemplate;

    public String render(EmailContentResponse content) {
        String rendered = loadTemplate();

        for (Map.Entry<String, String> token : buildTokens(content).entrySet()) {
            rendered = rendered.replace("{{" + token.getKey() + "}}", escapeHtml(token.getValue()));
        }

        if (rendered.contains("{{")) {
            // 템플릿에 토큰이 추가됐는데 서버가 따라가지 못한 경우다. 발송 자체는 막지 않되 눈에 띄게 남긴다.
            log.warn("치환되지 않은 토큰이 남아 있습니다 - sessionId={}", content.sessionId());
        }
        return rendered;
    }

    private Map<String, String> buildTokens(EmailContentResponse content) {
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("nickname", content.nickname());
        tokens.put("storeName", content.storeName());
        tokens.put("assetBaseUrl", content.assetBaseUrl());
        tokens.put("unsubscribeUrl", content.unsubscribeUrl());
        tokens.put("privacyUrl", content.privacyUrl());

        putSlotTokens(tokens, "pick", content.picks());
        putSlotTokens(tokens, "recommend", content.recommendations());
        return tokens;
    }

    private void putSlotTokens(Map<String, String> tokens, String prefix, List<EmailSlotItem> slots) {
        for (EmailSlotItem slot : slots) {
            tokens.put(prefix + "Name" + slot.slotOrder(), slot.productName());
            tokens.put(prefix + "Desc" + slot.slotOrder(), slot.description());
            tokens.put(prefix + "ImageUrl" + slot.slotOrder(), slot.imageUrl());
        }
    }

    private String loadTemplate() {
        String template = cachedTemplate;
        if (template != null) {
            return template;
        }

        Resource resource = resourceLoader.getResource(emailProperties.getTemplatePath());
        try (InputStream inputStream = resource.getInputStream()) {
            template = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("메일 템플릿을 읽지 못했습니다 - path={}", emailProperties.getTemplatePath(), e);
            throw new BusinessException(ErrorCode.EMAIL_TEMPLATE_NOT_FOUND);
        }

        cachedTemplate = template;
        return template;
    }

    /**
     * 상품명에 &, ", < 같은 문자가 섞여도 속성값(src, alt)과 본문 양쪽에서 안전하도록 이스케이프한다.
     * 값은 전부 상품명·URL 같은 평문이므로 HTML 조각을 주입할 일은 없다.
     */
    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
