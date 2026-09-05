package br.com.naheroback.common.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "rate-limit.email-dispatch")
public class RateLimitProperties {

    private Bucket perRecipient = new Bucket(20, Duration.ofHours(1));

    public RateLimitPolicy recipientPolicy() {
        return new RateLimitPolicy("email-dispatch:recipient", perRecipient.limit, perRecipient.period);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Bucket {
        private int limit;
        private Duration period;
    }
}
