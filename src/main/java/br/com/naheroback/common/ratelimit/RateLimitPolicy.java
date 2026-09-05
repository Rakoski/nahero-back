package br.com.naheroback.common.ratelimit;

import java.time.Duration;

public record RateLimitPolicy(String name, int limit, Duration period) {
}
