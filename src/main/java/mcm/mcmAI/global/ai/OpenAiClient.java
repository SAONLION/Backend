package mcm.mcmAI.global.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class OpenAiClient {

    private static final int MAX_TOKENS = 400;
    private static final double TEMPERATURE = 0.3;
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 8_000;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final String embeddingModel;

    public OpenAiClient(
            @Value("${openai.base-url}") String baseUrl,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.model}") String model,
            @Value("${openai.embedding-model}") String embeddingModel
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MS);

        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.apiKey = apiKey;
        this.model = model;
        this.embeddingModel = embeddingModel;
    }

    // 실패해도 예외를 던지지 않고 빈 Optional을 반환한다 — 호출부는 이를 fallback 신호로 사용한다.
    public Optional<String> requestChatCompletion(String systemPrompt, String userPrompt, boolean jsonResponse) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY가 설정되지 않아 OpenAI 호출을 건너뜁니다.");
            return Optional.empty();
        }

        try {
            ChatCompletionRequest request = new ChatCompletionRequest(
                    model,
                    List.of(new ChatMessage("system", systemPrompt), new ChatMessage("user", userPrompt)),
                    MAX_TOKENS,
                    TEMPERATURE,
                    jsonResponse ? new ResponseFormat("json_object") : null
            );

            ChatCompletionResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            return Optional.ofNullable(response).flatMap(ChatCompletionResponse::firstMessageContent);
        } catch (RestClientException | IllegalStateException e) {
            log.warn("OpenAI API 호출에 실패했습니다: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // 실패해도 예외를 던지지 않고 빈 Optional을 반환한다 — 호출부는 이를 fallback 신호로 사용한다.
    public Optional<float[]> requestEmbedding(String input) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY가 설정되지 않아 OpenAI 임베딩 호출을 건너뜁니다.");
            return Optional.empty();
        }

        try {
            EmbeddingRequest request = new EmbeddingRequest(embeddingModel, input);

            EmbeddingResponse response = restClient.post()
                    .uri("/embeddings")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(EmbeddingResponse.class);

            return Optional.ofNullable(response).flatMap(EmbeddingResponse::firstEmbedding);
        } catch (RestClientException | IllegalStateException e) {
            log.warn("OpenAI 임베딩 API 호출에 실패했습니다: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private record EmbeddingRequest(String model, String input) {
    }

    private record EmbeddingData(float[] embedding) {
    }

    private record EmbeddingResponse(List<EmbeddingData> data) {
        Optional<float[]> firstEmbedding() {
            if (data == null || data.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(data.get(0).embedding());
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            @JsonProperty("max_tokens") int maxTokens,
            double temperature,
            @JsonProperty("response_format") ResponseFormat responseFormat
    ) {
    }

    private record ResponseFormat(String type) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ChatCompletionResponse(List<Choice> choices) {
        Optional<String> firstMessageContent() {
            if (choices == null || choices.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(choices.get(0).message()).map(ChatMessage::content);
        }
    }

    private record Choice(ChatMessage message) {
    }
}