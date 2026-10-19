package br.com.naheroback.common.exceptions.custom;

import lombok.Getter;

@Getter
public class CaptchaRequiredException extends RuntimeException {

    public static final String ERROR_CODE = "CAPTCHA_REQUIRED";

    private static final String MESSAGE_KEY = "common.captcha.required";

    private final String messageKey;

    public CaptchaRequiredException() {
        super(MESSAGE_KEY);
        this.messageKey = MESSAGE_KEY;
    }
}
