package mcm.mcmAI.global.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmbeddingCodec {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String encode(float[] embedding) {
        try {
            return OBJECT_MAPPER.writeValueAsString(embedding);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("임베딩 직렬화에 실패했습니다.", e);
        }
    }

    // 저장된 값이 없거나 손상되어 파싱에 실패하면 null을 반환한다 — 호출부는 이를 건너뛰기 신호로 사용한다.
    public float[] decode(String embedding) {
        if (embedding == null || embedding.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(embedding, float[].class);
        } catch (Exception e) {
            log.warn("임베딩 역직렬화에 실패했습니다: {}", e.getMessage());
            return null;
        }
    }
}
