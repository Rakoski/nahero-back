package br.com.naheroback.common.exceptions.custom;

import lombok.Getter;

@Getter
public class PaymentRequiredException extends RuntimeException {
    private final String messageKey;
    private final Object[] args;

    public PaymentRequiredException(String messageKey, Object... args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args;
    }
}
