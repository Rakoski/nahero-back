package br.com.naheroback.providers.deepseek;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j(topic = "DEEPSEEK")
@Component
public class DeepSeekClient {

    private static final int MAX_ATTEMPTS = 2;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final String apiKey;
    private final String model;

    public DeepSeekClient(@Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
                          @Value("${deepseek.api-key:}") String apiKey,
                          @Value("${deepseek.model:deepseek-flash}") String model,
                          @Value("${deepseek.timeout:30s}") Duration timeout,
                          ObjectMapper objectMapper,
                          Validator validator) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String model() {
        return model;
    }

    public <T> T completeJson(String systemPrompt, String userPrompt, Class<T> responseType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("DEEPSEEK_API_KEY is not configured");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)),
                "response_format", Map.of("type", "json_object"),
                "stream", false);

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return call(body, responseType);
            } catch (RestClientException | IllegalStateException e) {
                lastFailure = e;
                log.warn("DeepSeek call {}/{} failed: {}", attempt, MAX_ATTEMPTS, e.getMessage());
            }
        }
        throw new IllegalStateException("DeepSeek call failed after %d attempts".formatted(MAX_ATTEMPTS), lastFailure);
    }

    private <T> T call(Map<String, Object> body, Class<T> responseType) {
        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        JsonNode content = response == null ? null : response.at("/choices/0/message/content");
        if (content == null || !content.isTextual() || content.asText().isBlank()) {
            throw new IllegalStateException("DeepSeek returned no message content");
        }

        T value;
        try {
            value = objectMapper.readerFor(responseType)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(content.asText());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("DeepSeek returned content that does not map to " + responseType.getSimpleName(), e);
        }

        if (value == null) throw new IllegalStateException("DeepSeek returned an empty " + responseType.getSimpleName());

        Set<ConstraintViolation<T>> violations = validator.validate(value);
        if (!violations.isEmpty()) {
            throw new IllegalStateException("DeepSeek returned an invalid %s: %s".formatted(
                    responseType.getSimpleName(),
                    violations.stream()
                            .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                            .sorted()
                            .collect(Collectors.joining(", "))));
        }

        return value;
    }
}
