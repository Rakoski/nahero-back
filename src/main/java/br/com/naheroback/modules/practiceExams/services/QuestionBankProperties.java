package br.com.naheroback.modules.practiceExams.services;

import br.com.naheroback.common.ratelimit.RateLimitPolicy;
import br.com.naheroback.common.ratelimit.RateLimitProperties.Bucket;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "question-bank")
public class QuestionBankProperties {

    private Bucket burst = new Bucket(60, Duration.ofMinutes(10));
    private Bucket daily = new Bucket(600, Duration.ofHours(24));
    private int maxPageSize = 20;
    private int previewLength = 200;
    private int maxLists = 50;
    private int maxListSize = 200;
    private int filterScanLimit = 5000;

    public RateLimitPolicy burstPolicy() {
        return new RateLimitPolicy("question-bank:burst", burst.getLimit(), burst.getPeriod());
    }

    public RateLimitPolicy dailyPolicy() {
        return new RateLimitPolicy("question-bank:daily", daily.getLimit(), daily.getPeriod());
    }
}
