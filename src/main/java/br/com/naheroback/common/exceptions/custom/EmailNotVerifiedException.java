package br.com.naheroback.common.exceptions.custom;

import lombok.Getter;

@Getter
public class EmailNotVerifiedException extends RuntimeException {

    public static final String ERROR_CODE = "EMAIL_NOT_VERIFIED";

    private static final String MESSAGE_KEY = "auth.email_verification.not_verified";

    private final String messageKey;

    public EmailNotVerifiedException() {
        super(MESSAGE_KEY);
        this.messageKey = MESSAGE_KEY;
    }
}
