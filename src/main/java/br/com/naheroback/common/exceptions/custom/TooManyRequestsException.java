package br.com.naheroback.common.exceptions.custom;

import lombok.Getter;

@Getter
public class TooManyRequestsException extends RuntimeException {

    public static final String ERROR_CODE = "TOO_MANY_REQUESTS";

    private static final String MESSAGE_KEY = "common.rate_limit.exceeded";

    private final String messageKey;

    public TooManyRequestsException() {
        super(MESSAGE_KEY);
        this.messageKey = MESSAGE_KEY;
    }
}
