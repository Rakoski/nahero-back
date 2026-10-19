package br.com.naheroback.providers.turnstile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

@Slf4j(topic = "TURNSTILE")
@Component
public class TurnstileClient {

    private static final int MAX_TOKEN_LENGTH = 2048;

    private final RestClient restClient;
    private final String secretKey;

    public TurnstileClient(@Value("${turnstile.base-url:https://challenges.cloudflare.com}") String baseUrl,
                           @Value("${turnstile.secret-key:}") String secretKey,
                           @Value("${turnstile.timeout:5s}") Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.secretKey = secretKey;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SiteVerifyResponse(boolean success, @JsonProperty("error-codes") List<String> errorCodes) {}

    public boolean verify(String token) {
        if (secretKey == null || secretKey.isBlank()) {
            log.warn("TURNSTILE_SECRET_KEY is not configured; rejecting captcha token");
            return false;
        }

        if (token == null || token.isBlank() || token.length() > MAX_TOKEN_LENGTH) return false;


        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secretKey);
        form.add("response", token);

        try {
            SiteVerifyResponse response = restClient.post()
                    .uri("/turnstile/v0/siteverify")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(SiteVerifyResponse.class);

            if (response == null || !response.success()) {
                log.info("Captcha rejected: {}", response == null ? "empty response" : response.errorCodes());
                return false;
            }
            return true;
        } catch (RestClientException e) {
            log.warn("Captcha verification failed: {}", e.getMessage());
            return false;
        }
    }
}
