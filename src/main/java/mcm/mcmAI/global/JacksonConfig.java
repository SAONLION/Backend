package mcm.mcmAI.global;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonConfig {

    // JVM 기본 타임존을 Asia/Seoul로 고정했으므로(McmAiApplication), 여기서 다루는 LocalDateTime 값은
    // 이미 KST 벽시계 값이다. 직렬화 시 그 사실을 +09:00으로 명시해서 프론트가 UTC로 오인하지 않게 한다.
    private static final ZoneOffset KST_OFFSET = ZoneOffset.of("+09:00");

    @Bean
    public JsonMapperBuilderCustomizer kstOffsetLocalDateTimeCustomizer() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new KstOffsetLocalDateTimeSerializer());
        return builder -> builder.addModule(module);
    }

    private static class KstOffsetLocalDateTimeSerializer extends ValueSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeString(value.atOffset(KST_OFFSET).toString());
        }
    }
}
