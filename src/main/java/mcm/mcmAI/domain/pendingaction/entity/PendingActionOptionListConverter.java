package mcm.mcmAI.domain.pendingaction.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class PendingActionOptionListConverter implements AttributeConverter<List<PendingActionOption>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<PendingActionOption> attribute) {
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("옵션 목록을 직렬화하지 못했습니다.", e);
        }
    }

    @Override
    public List<PendingActionOption> convertToEntityAttribute(String dbData) {
        try {
            return OBJECT_MAPPER.readValue(dbData, new TypeReference<List<PendingActionOption>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("옵션 목록을 역직렬화하지 못했습니다.", e);
        }
    }
}