package br.com.naheroback.modules.practiceExams.services;

import br.com.naheroback.common.exceptions.custom.CaptchaRequiredException;
import br.com.naheroback.common.exceptions.custom.TooManyRequestsException;
import br.com.naheroback.common.ratelimit.RateLimitService;
import br.com.naheroback.providers.turnstile.TurnstileClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionBankThrottle {

    private final RateLimitService rateLimitService;
    private final TurnstileClient turnstileClient;
    private final QuestionBankProperties properties;

    public void check(Integer studentId, String captchaToken) {
        String key = String.valueOf(studentId);

        if (captchaToken != null && !captchaToken.isBlank()) {
            if (!turnstileClient.verify(captchaToken)) {
                throw new CaptchaRequiredException();
            }
            rateLimitService.reset(properties.burstPolicy(), key);
        }

        try {
            rateLimitService.consume(properties.burstPolicy(), key);
        } catch (TooManyRequestsException e) {
            log.info("Question bank burst limit reached for student {}", studentId);
            throw new CaptchaRequiredException();
        }

        rateLimitService.consume(properties.dailyPolicy(), key);
    }
}
