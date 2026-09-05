package br.com.naheroback.common.ratelimit;

import br.com.naheroback.common.exceptions.custom.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    private static final RateLimitPolicy POLICY = new RateLimitPolicy("test", 3, Duration.ofHours(1));

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
    }

    @Test
    @DisplayName("Should allow calls up to the configured limit")
    void shouldAllowCallsUpToLimit() {
        assertDoesNotThrow(() -> {
            for (int attempt = 0; attempt < POLICY.limit(); attempt++) {
                rateLimitService.consume(POLICY, "someone@example.com");
            }
        });
    }

    @Test
    @DisplayName("Should reject the call that goes past the limit")
    void shouldRejectCallPastLimit() {
        for (int attempt = 0; attempt < POLICY.limit(); attempt++) {
            rateLimitService.consume(POLICY, "someone@example.com");
        }

        assertThrows(TooManyRequestsException.class,
                () -> rateLimitService.consume(POLICY, "someone@example.com"));
    }

    @Test
    @DisplayName("Should count each key separately")
    void shouldCountKeysSeparately() {
        for (int attempt = 0; attempt < POLICY.limit(); attempt++) {
            rateLimitService.consume(POLICY, "someone@example.com");
        }

        assertDoesNotThrow(() -> rateLimitService.consume(POLICY, "someone-else@example.com"));
    }

    @Test
    @DisplayName("Should treat keys that differ only by case or padding as one caller")
    void shouldNormalizeKeys() {
        rateLimitService.consume(POLICY, "Someone@Example.com");
        rateLimitService.consume(POLICY, "  someone@example.com  ");
        rateLimitService.consume(POLICY, "SOMEONE@EXAMPLE.COM");

        assertThrows(TooManyRequestsException.class,
                () -> rateLimitService.consume(POLICY, "someone@example.com"));
    }

    @Test
    @DisplayName("Should keep separate budgets per policy")
    void shouldKeepSeparateBudgetsPerPolicy() {
        RateLimitPolicy other = new RateLimitPolicy("other", 3, Duration.ofHours(1));

        for (int attempt = 0; attempt < POLICY.limit(); attempt++) {
            rateLimitService.consume(POLICY, "someone@example.com");
        }

        assertDoesNotThrow(() -> rateLimitService.consume(other, "someone@example.com"));
    }
}
