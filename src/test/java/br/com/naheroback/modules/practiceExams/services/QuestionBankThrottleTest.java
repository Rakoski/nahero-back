package br.com.naheroback.modules.practiceExams.services;

import br.com.naheroback.common.exceptions.custom.CaptchaRequiredException;
import br.com.naheroback.common.exceptions.custom.TooManyRequestsException;
import br.com.naheroback.common.ratelimit.RateLimitProperties.Bucket;
import br.com.naheroback.common.ratelimit.RateLimitService;
import br.com.naheroback.providers.turnstile.TurnstileClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuestionBankThrottleTest {

    private static final int STUDENT = 42;

    private TurnstileClient turnstileClient;
    private QuestionBankThrottle throttle;

    @BeforeEach
    void setUp() {
        QuestionBankProperties properties = new QuestionBankProperties();
        properties.setBurst(new Bucket(2, Duration.ofHours(1)));
        properties.setDaily(new Bucket(5, Duration.ofHours(24)));

        turnstileClient = mock(TurnstileClient.class);
        throttle = new QuestionBankThrottle(new RateLimitService(), turnstileClient, properties);
    }

    @Test
    @DisplayName("Should let requests through while the burst budget lasts, without asking for a captcha")
    void shouldAllowWithinBurst() {
        assertDoesNotThrow(() -> {
            throttle.check(STUDENT, null);
            throttle.check(STUDENT, null);
        });
        verifyNoInteractions(turnstileClient);
    }

    @Test
    @DisplayName("Should ask for a captcha once the burst budget runs out")
    void shouldRequireCaptchaPastBurst() {
        throttle.check(STUDENT, null);
        throttle.check(STUDENT, null);

        assertThrows(CaptchaRequiredException.class, () -> throttle.check(STUDENT, null));
    }

    @Test
    @DisplayName("Should restore the burst budget when a valid captcha token comes in")
    void shouldResetBurstOnValidCaptcha() {
        when(turnstileClient.verify("good")).thenReturn(true);
        throttle.check(STUDENT, null);
        throttle.check(STUDENT, null);

        assertDoesNotThrow(() -> {
            throttle.check(STUDENT, "good");
            throttle.check(STUDENT, null);
        });
        assertThrows(CaptchaRequiredException.class, () -> throttle.check(STUDENT, null));
    }

    @Test
    @DisplayName("Should keep asking for a captcha when the token is rejected")
    void shouldRejectInvalidCaptcha() {
        when(turnstileClient.verify("bad")).thenReturn(false);
        throttle.check(STUDENT, null);
        throttle.check(STUDENT, null);

        assertThrows(CaptchaRequiredException.class, () -> throttle.check(STUDENT, "bad"));
        assertThrows(CaptchaRequiredException.class, () -> throttle.check(STUDENT, null));
    }

    @Test
    @DisplayName("Should stop the student at the daily cap even with a valid captcha")
    void shouldEnforceDailyCapRegardlessOfCaptcha() {
        when(turnstileClient.verify("good")).thenReturn(true);
        for (int request = 0; request < 5; request++) {
            throttle.check(STUDENT, "good");
        }

        assertThrows(TooManyRequestsException.class, () -> throttle.check(STUDENT, "good"));
    }

    @Test
    @DisplayName("Should count each student separately")
    void shouldCountStudentsSeparately() {
        throttle.check(STUDENT, null);
        throttle.check(STUDENT, null);

        assertDoesNotThrow(() -> throttle.check(STUDENT + 1, null));
    }
}
