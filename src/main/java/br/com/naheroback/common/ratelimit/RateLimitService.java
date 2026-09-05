package br.com.naheroback.common.ratelimit;

import br.com.naheroback.common.exceptions.custom.TooManyRequestsException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Slf4j(topic = "RATE_LIMIT")
@Service
public class RateLimitService {

    private static final int MAXIMUM_TRACKED_KEYS = 100_000;
    private static final Duration IDLE_RETENTION = Duration.ofHours(1);

    private final Cache<String, RateLimiter> limiters = Caffeine.newBuilder()
            .maximumSize(MAXIMUM_TRACKED_KEYS)
            .expireAfterAccess(IDLE_RETENTION)
            .build();

    public void consume(RateLimitPolicy policy, String key) {
        String normalizedKey = "%s:%s".formatted(policy.name(), key.trim().toLowerCase(Locale.ROOT));
        RateLimiter limiter = limiters.get(normalizedKey, name -> RateLimiter.of(name, configFor(policy)));

        if (!limiter.acquirePermission()) throw new TooManyRequestsException();
    }

    private RateLimiterConfig configFor(RateLimitPolicy policy) {
        return RateLimiterConfig.custom()
                .limitForPeriod(policy.limit())
                .limitRefreshPeriod(policy.period())
                .timeoutDuration(Duration.ZERO)
                .build();
    }
}
